package com.ror.nms2go

import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import java.util.zip.ZipInputStream

data class GmailAttachmentPart(
    val fileName: String,
    val mimeType: String?,
    val attachmentId: String?,
    val inlineData: String?,
    val contentDisposition: String?
)

object GmailAttachmentUtils {
    fun firstAttachment(parts: Sequence<GmailAttachmentPart>): GmailAttachmentPart? {
        return parts.firstOrNull { it.isDownloadableAttachment() }
    }

    fun firstExcelAttachment(parts: Sequence<GmailAttachmentPart>): GmailAttachmentPart? {
        return parts.firstOrNull { it.isDownloadableAttachment() && it.isExcelFile() }
    }

    private fun GmailAttachmentPart.isExcelFile(): Boolean {
        val name = fileName.lowercase()
        return name.endsWith(".xls") || name.endsWith(".xlsx")
    }

    private fun GmailAttachmentPart.isDownloadableAttachment(): Boolean {
        if (fileName.isBlank()) {
            return false
        }
        if (attachmentId.isNullOrBlank() && inlineData.isNullOrBlank()) {
            return false
        }

        val disposition = contentDisposition.orEmpty().lowercase()
        if (disposition.contains("attachment")) {
            return true
        }
        if (disposition.contains("inline")) {
            return false
        }

        return true
    }

    fun isZipArchive(fileName: String, mimeType: String?): Boolean {
        val name = fileName.lowercase()
        return name.endsWith(".zip") ||
            mimeType.equals("application/zip", ignoreCase = true) ||
            mimeType.equals("application/x-zip-compressed", ignoreCase = true)
    }

    fun extractExcelFromZip(zipBytes: ByteArray, outputDirectory: File): File? {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name.substringAfterLast('/')
                if (!entry.isDirectory &&
                    !entryName.startsWith("._") &&
                    (entryName.endsWith(".xls") || entryName.endsWith(".xlsx"))
                ) {
                    val outputFile = File(outputDirectory, sanitizeFileName(entryName))
                    outputFile.writeBytes(zip.readBytes())
                    return outputFile
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    fun sanitizeFileName(fileName: String): String {
        val sanitized = fileName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
        return sanitized.ifBlank { "attachment.bin" }
    }

    fun decodeBase64Url(data: String): ByteArray {
        val padding = (4 - data.length % 4) % 4
        val normalized = data + "=".repeat(padding)
        return Base64.getUrlDecoder().decode(normalized)
    }

    fun encodeBase64Url(data: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }
}
