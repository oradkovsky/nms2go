package com.ror.nms2go

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailAttachmentUtilsTest {
    @Test
    fun firstAttachment_skipsBlankFilenames() {
        val result = GmailAttachmentUtils.firstAttachment(
            sequenceOf(
                GmailAttachmentPart("", "text/plain", "1", null, null),
                GmailAttachmentPart("report.pdf", "application/pdf", "2", null, "attachment; filename=report.pdf")
            )
        )

        assertEquals("report.pdf", result?.fileName)
    }

    @Test
    fun firstAttachment_returnsNullWhenNoAttachmentExists() {
        val result = GmailAttachmentUtils.firstAttachment(
            sequenceOf(
                GmailAttachmentPart("", null, null, null, null),
                GmailAttachmentPart("   ", null, null, null, null)
            )
        )

        assertNull(result)
    }

    @Test
    fun firstAttachment_skipsInlineParts() {
        val result = GmailAttachmentUtils.firstAttachment(
            sequenceOf(
                GmailAttachmentPart("logo.png", "image/png", "1", null, "inline; filename=logo.png"),
                GmailAttachmentPart("invoice.pdf", "application/pdf", "2", null, "attachment; filename=invoice.pdf")
            )
        )

        assertEquals("invoice.pdf", result?.fileName)
    }

    @Test
    fun firstExcelAttachment_prefersXlsWhenMultipleAttachments() {
        val result = GmailAttachmentUtils.firstExcelAttachment(
            sequenceOf(
                GmailAttachmentPart("price.dbf", "application/x-dbf", "1", null, "attachment; filename=price.dbf"),
                GmailAttachmentPart("price.xls", "application/vnd.ms-excel", "2", null, "attachment; filename=price.xls")
            )
        )

        assertEquals("price.xls", result?.fileName)
    }

    @Test
    fun firstExcelAttachment_matchesXlsx() {
        val result = GmailAttachmentUtils.firstExcelAttachment(
            sequenceOf(
                GmailAttachmentPart("report.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "3", null, "attachment; filename=report.xlsx")
            )
        )

        assertEquals("report.xlsx", result?.fileName)
    }

    @Test
    fun firstExcelAttachment_returnsNullWhenNoExcelAttachment() {
        val result = GmailAttachmentUtils.firstExcelAttachment(
            sequenceOf(
                GmailAttachmentPart("price.dbf", "application/x-dbf", "1", null, "attachment; filename=price.dbf"),
                GmailAttachmentPart("invoice.pdf", "application/pdf", "2", null, "attachment; filename=invoice.pdf")
            )
        )

        assertNull(result)
    }

    @Test
    fun sanitizeFileName_replacesUnsupportedCharacters() {
        val sanitized = GmailAttachmentUtils.sanitizeFileName("quarterly report:Q3/2026?.pdf")

        assertEquals("quarterly_report_Q3_2026_.pdf", sanitized)
    }

    @Test
    fun decodeBase64Url_decodesUnpaddedContent() {
        val decoded = GmailAttachmentUtils.decodeBase64Url("SGVsbG8")

        assertArrayEquals("Hello".toByteArray(), decoded)
    }

    @Test
    fun isZipArchive_matchesExtensionAndMimeTypes() {
        assertTrue(GmailAttachmentUtils.isZipArchive("price.zip", null))
        assertTrue(GmailAttachmentUtils.isZipArchive("PRICE.ZIP", "application/zip"))
        assertTrue(GmailAttachmentUtils.isZipArchive("price.zip.part", "application/x-zip-compressed"))
        assertFalse(GmailAttachmentUtils.isZipArchive("price.xls", "application/vnd.ms-excel"))
        assertFalse(GmailAttachmentUtils.isZipArchive("price.xlsx", null))
    }

    @Test
    fun extractExcelFromZip_extractsXlsFromArchive() {
        val tempDir = Files.createTempDirectory("zip-test").toFile()
        try {
            val xlsBytes = "MOCK-XLS-CONTENT".toByteArray(StandardCharsets.UTF_8)
            val result = GmailAttachmentUtils.extractExcelFromZip(
                zipOf("price.xls" to xlsBytes),
                tempDir
            )

            assertEquals("price.xls", result?.name)
            assertArrayEquals(xlsBytes, result?.readBytes())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun extractExcelFromZip_handlesNestedFoldersAndMetadata() {
        val tempDir = Files.createTempDirectory("zip-test").toFile()
        try {
            val xlsBytes = "CONTENT".toByteArray(StandardCharsets.UTF_8)
            val path = "reports/2026/price.xlsx"
            val result = GmailAttachmentUtils.extractExcelFromZip(
                zipOf(
                    "__MACOSX/._price.xlsx" to "junk".toByteArray(),
                    path to xlsBytes
                ),
                tempDir
            )

            assertEquals("price.xlsx", result?.name)
            assertArrayEquals(xlsBytes, result?.readBytes())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun extractExcelFromZip_returnsNullWhenNoExcelInside() {
        val tempDir = Files.createTempDirectory("zip-test").toFile()
        try {
            val result = GmailAttachmentUtils.extractExcelFromZip(
                zipOf("readme.txt" to "hello".toByteArray()),
                tempDir
            )

            assertNull(result)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }
}
