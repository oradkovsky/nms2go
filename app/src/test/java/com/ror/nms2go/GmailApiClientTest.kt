package com.ror.nms2go

import com.ror.nms2go.data.GmailApiClient
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailApiClientTest {

    private val client = GmailApiClient(File("unused"))
    private fun subjectLine(raw: String): String =
        raw.lineSequence().first { it.startsWith("Subject:") }

    @Test
    fun buildRawMessage_keepsAsciiSubjectAsIs() {
        val subject = "Order Acme 2026-08-17 10:00"
        val raw = client.buildRawMessage("receiver@example.com", subject, "<p>hi</p>")

        assertEquals("Subject: $subject", subjectLine(raw))
        assertTrue(raw.startsWith("To: receiver@example.com\r\n"))
        assertTrue(raw.contains("Content-Type: text/html; charset=UTF-8\r\n"))
    }

    @Test
    fun buildRawMessage_rfc2047EncodesUnicodeSubject() {
        val subject = "Замовлення Acme 2026-08-17 10:00"
        val raw = client.buildRawMessage("receiver@example.com", subject, "<p>hi</p>")

        val line = subjectLine(raw)
        assertTrue(line.startsWith("Subject: =?UTF-8?B?"))
        assertTrue(line.endsWith("?="))

        val encoded = line.removePrefix("Subject: =?UTF-8?B?").removeSuffix("?=")
        val decoded = String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
        assertEquals(subject, decoded)
    }

    @Test
    fun writeAttachment_extractsExcelFromZipPart() {
        val tempDir = Files.createTempDirectory("attachment-test").toFile()
        try {
            val client = GmailApiClient(tempDir)
            val xlsBytes = "PRICE".toByteArray(StandardCharsets.UTF_8)
            val zipBytes = ByteArrayOutputStream().use { buffer ->
                ZipOutputStream(buffer).use { zip ->
                    zip.putNextEntry(ZipEntry("price.xls"))
                    zip.write(xlsBytes)
                    zip.closeEntry()
                }
                buffer.toByteArray()
            }

            val result = client.writeAttachment(
                GmailAttachmentPart("price.zip", "application/zip", null, null, "attachment; filename=price.zip"),
                zipBytes
            )

            assertEquals("price.xls", result.name)
            assertTrue(xlsBytes.contentEquals(result.readBytes()))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun priceListSearch_usesAttachmentAndFileTypeFiltersWithOneResult() {
        val query = GmailApiClient.priceListQuery("supplier@example.com")

        assertEquals(
            "from:supplier@example.com has:attachment {filename:xls filename:xlsx filename:zip}",
            query
        )
        assertEquals(
            "/gmail/v1/users/me/messages?q=from%3Asupplier%40example.com+has%3Aattachment+%7Bfilename%3Axls+filename%3Axlsx+filename%3Azip%7D&maxResults=1",
            GmailApiClient.messageListPath(query)
        )
    }

    @Test
    fun parseSkipKeywords_splitsOnCommaSemicolonAndNewline() {
        assertEquals(
            listOf("відмови", "повернення", "return", "next"),
            GmailApiClient.parseSkipKeywords("відмови, повернення; return\nnext")
        )
        assertEquals(emptyList<String>(), GmailApiClient.parseSkipKeywords(""))
        assertEquals(emptyList<String>(), GmailApiClient.parseSkipKeywords("  , ; \n "))
        assertEquals(listOf("відмови"), GmailApiClient.parseSkipKeywords("  відмови  "))
    }

    @Test
    fun subjectMatchesSkip_isCaseInsensitiveAndBlankSafe() {
        assertTrue(GmailApiClient.subjectMatchesSkip("Прайс Вента відмови", listOf("відмови")))
        assertTrue(GmailApiClient.subjectMatchesSkip("ВІДМОВИ", listOf("відмови")))
        assertTrue(
            GmailApiClient.subjectMatchesSkip(
                "Price with return note",
                listOf("відмови", "return")
            )
        )
        assertFalse(GmailApiClient.subjectMatchesSkip("Прайс Вента від 01.01.2024", listOf("відмови")))
        assertFalse(GmailApiClient.subjectMatchesSkip("Price list", listOf("відмови")))
        assertFalse(GmailApiClient.subjectMatchesSkip(null, listOf("відмови")))
        assertFalse(GmailApiClient.subjectMatchesSkip("", listOf("відмови")))
        assertFalse(GmailApiClient.subjectMatchesSkip("Прайс відмови", emptyList()))
    }

    @Test
    fun skipSearch_requestsMoreCandidatesThanDefault() {
        val query = GmailApiClient.priceListQuery("venta@example.com")
        val defaultPath = GmailApiClient.messageListPath(query)
        val skipPath = GmailApiClient.messageListPath(query, GmailApiClient.SKIP_SEARCH_MAX_RESULTS)

        assertTrue(defaultPath.endsWith("maxResults=1"))
        assertTrue(skipPath.endsWith("maxResults=${GmailApiClient.SKIP_SEARCH_MAX_RESULTS}"))
        assertTrue(GmailApiClient.SKIP_SEARCH_MAX_RESULTS > 1)
    }
}
