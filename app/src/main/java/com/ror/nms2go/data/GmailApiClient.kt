package com.ror.nms2go.data

import com.ror.nms2go.EmailDateParser
import com.ror.nms2go.GmailAttachmentPart
import com.ror.nms2go.GmailAttachmentUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

data class SenderOverview(
    val senderQuery: String,
    val messageId: String?,
    val subject: String?,
    val from: String?,
    val date: String?,
    val hasAttachment: Boolean,
    val status: Status,
    val attachment: GmailAttachmentPart? = null
) {
    enum class Status {
        FOUND,
        NO_MESSAGES,
        NO_ATTACHMENTS
    }
}

interface GmailTransport {
    fun getJson(path: String, accessToken: String): JSONObject

    fun postJson(path: String, payload: JSONObject, accessToken: String)
}

private class HttpGmailTransport : GmailTransport {
    override fun getJson(path: String, accessToken: String): JSONObject {
        return withRetry { getJsonOnce(path, accessToken) }
    }

    override fun postJson(path: String, payload: JSONObject, accessToken: String) {
        // Retrying a timed-out send can create a duplicate order email, so only idempotent GETs retry.
        postJsonOnce(path, payload, accessToken)
    }

    private fun getJsonOnce(path: String, accessToken: String): JSONObject {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return connection.useJsonResponse()
    }

    private fun postJsonOnce(path: String, payload: JSONObject, accessToken: String) {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                throw GmailApiException(status, body)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun HttpURLConnection.useJsonResponse(): JSONObject {
        return try {
            val body = (if (responseCode in 200..299) inputStream else errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (responseCode !in 200..299) {
                throw GmailApiException(responseCode, body)
            }
            JSONObject(body)
        } finally {
            disconnect()
        }
    }

    private fun <T> withRetry(request: () -> T): T {
        var lastFailure: IOException? = null
        repeat(MAX_REQUEST_ATTEMPTS) { attempt ->
            try {
                return request()
            } catch (error: IOException) {
                if (!error.isRetryable() || attempt == MAX_REQUEST_ATTEMPTS - 1) {
                    throw error
                }
                lastFailure = error
                try {
                    Thread.sleep(RETRY_BACKOFF_MILLIS * (1L shl attempt))
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw error
                }
            }
        }
        throw checkNotNull(lastFailure)
    }

    private fun IOException.isRetryable(): Boolean =
        this !is GmailApiException || statusCode == 429 || statusCode in 500..599

    private class GmailApiException(statusCode: Int, body: String) :
        IOException("Gmail API request failed with HTTP $statusCode: $body") {
        val statusCode = statusCode
    }

    private companion object {
        const val BASE_URL = "https://gmail.googleapis.com"
        const val MAX_REQUEST_ATTEMPTS = 3
        const val RETRY_BACKOFF_MILLIS = 250L
    }
}

class GmailApiClient(
    private val outputDirectory: File,
    private val transport: GmailTransport = HttpGmailTransport()
) {
    fun loadOverview(
        senders: List<String>,
        accessToken: String
    ): List<SenderOverview> {
        if (senders.isEmpty()) return emptyList()
        val executor = Executors.newFixedThreadPool(minOf(senders.size, MAX_PARALLEL_GMAIL_REQUESTS))
        try {
            val lookups = senders.map { sender ->
                executor.submit<SenderOverview> { loadOverviewForSender(sender, accessToken) }
            }
            return lookups.map { lookup ->
                try {
                    lookup.get()
                } catch (error: ExecutionException) {
                    throw error.cause ?: error
                }
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun loadOverviewForSender(sender: String, accessToken: String): SenderOverview {
        val attachmentMessageId = latestMessageId(
            query = priceListQuery(sender),
            accessToken = accessToken
        )
        if (attachmentMessageId == null) {
            val status = if (latestMessageId("from:$sender", accessToken) == null) {
                SenderOverview.Status.NO_MESSAGES
            } else {
                SenderOverview.Status.NO_ATTACHMENTS
            }
            return SenderOverview(
                senderQuery = sender,
                messageId = null,
                subject = null,
                from = null,
                date = null,
                hasAttachment = false,
                status = status
            )
        }

        val message = transport.getJson(
            path = "/gmail/v1/users/me/messages/$attachmentMessageId?format=full",
            accessToken = accessToken
        )
        val payload = message.optJSONObject("payload") ?: JSONObject()
        val part = GmailAttachmentUtils.firstExcelAttachment(flattenAttachments(payload))
            ?: GmailAttachmentUtils.firstAttachment(flattenAttachments(payload))
        val rawDateHeader = headerValue(payload, "Date")
        val internalDateMillis = message.optString("internalDate").toLongOrNull()
        // Overview uses Gmail's internal date, not the device's current date.
        val overviewDate = EmailDateParser.formatForOverview(rawDateHeader, internalDateMillis)
        return SenderOverview(
            senderQuery = sender,
            messageId = part?.let { attachmentMessageId },
            subject = headerValue(payload, "Subject"),
            from = headerValue(payload, "From"),
            date = overviewDate,
            hasAttachment = part != null,
            status = if (part != null) SenderOverview.Status.FOUND else SenderOverview.Status.NO_ATTACHMENTS,
            attachment = part
        )
    }

    fun downloadAttachment(
        messageId: String,
        part: GmailAttachmentPart,
        accessToken: String
    ): File {
        val bytes = when {
            !part.inlineData.isNullOrBlank() -> GmailAttachmentUtils.decodeBase64Url(part.inlineData)
            !part.attachmentId.isNullOrBlank() -> {
                val attachmentBody = transport.getJson(
                    path = "/gmail/v1/users/me/messages/$messageId/attachments/${part.attachmentId}",
                    accessToken = accessToken
                )
                GmailAttachmentUtils.decodeBase64Url(attachmentBody.getString("data"))
            }

            else -> throw IOException("Attachment metadata exists but Gmail did not return attachment data.")
        }

        return writeAttachment(part, bytes)
    }

    fun sendMessage(to: String, subject: String, htmlBody: String, accessToken: String) {
        val raw = buildRawMessage(to, subject, htmlBody)
        val payload = JSONObject()
            .put("raw", GmailAttachmentUtils.encodeBase64Url(raw.toByteArray(StandardCharsets.UTF_8)))
        transport.postJson("/gmail/v1/users/me/messages/send", payload, accessToken)
    }

    internal fun writeAttachment(part: GmailAttachmentPart, bytes: ByteArray): File {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        if (GmailAttachmentUtils.isZipArchive(part.fileName, part.mimeType)) {
            return GmailAttachmentUtils.extractExcelFromZip(bytes, outputDirectory)
                ?: throw IOException("No Excel file found inside archive '${part.fileName}'")
        }
        return File(outputDirectory, GmailAttachmentUtils.sanitizeFileName(part.fileName))
            .apply { writeBytes(bytes) }
    }

    internal fun buildRawMessage(to: String, subject: String, htmlBody: String): String {
        return buildString {
            append("To: ").append(to).append("\r\n")
            append("Subject: ").append(encodeRfc2047(subject)).append("\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: text/html; charset=UTF-8\r\n")
            append("\r\n")
            append(htmlBody)
        }
    }

    private fun encodeRfc2047(value: String): String {
        if (value.all { it.code < 0x80 }) return value
        return "=?UTF-8?B?" +
            Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8)) +
            "?="
    }

    private fun flattenAttachments(part: JSONObject): Sequence<GmailAttachmentPart> = sequence {
        val fileName = part.optString("filename")
        if (fileName.isNotBlank()) {
            val body = part.optJSONObject("body")
            yield(
                GmailAttachmentPart(
                    fileName = fileName,
                    mimeType = part.optString("mimeType"),
                    attachmentId = body?.optString("attachmentId"),
                    inlineData = body?.optString("data"),
                    contentDisposition = headerValue(part, "Content-Disposition")
                )
            )
        }

        val parts = part.optJSONArray("parts") ?: JSONArray()
        for (index in 0 until parts.length()) {
            yieldAll(flattenAttachments(parts.getJSONObject(index)))
        }
    }

    private fun headerValue(payload: JSONObject, headerName: String): String? {
        val headers = payload.optJSONArray("headers") ?: return null
        for (index in 0 until headers.length()) {
            val header = headers.getJSONObject(index)
            if (headerName.equals(header.optString("name"), ignoreCase = true)) {
                return header.optString("value")
            }
        }
        return null
    }

    private fun latestMessageId(query: String, accessToken: String): String? {
        val response = transport.getJson(
            path = messageListPath(query),
            accessToken = accessToken
        )
        return response.optJSONArray("messages")
            ?.optJSONObject(0)
            ?.optString("id")
            ?.takeIf { it.isNotBlank() }
    }

    internal companion object {
        const val MAX_PARALLEL_GMAIL_REQUESTS = 3

        fun priceListQuery(sender: String): String =
            "from:$sender has:attachment {filename:xls filename:xlsx filename:zip}"

        fun messageListPath(query: String): String =
            "/gmail/v1/users/me/messages?q=${
                URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            }&maxResults=1"
    }
}
