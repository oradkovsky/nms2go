package com.ror.nms2go

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExcelParserTest {

    @Test
    fun supplierPrefix_matchesByPrefixAndName() {
        assertEquals("dekada", ExcelParser.supplierPrefix("dekada_prices_12-08-2026.xls"))
        assertEquals("alba", ExcelParser.supplierPrefix("ALBA_55.xls"))
        assertEquals("venta", ExcelParser.supplierPrefix("venta.xls"))
        assertEquals("dekada", ExcelParser.supplierPrefix("прайс_Декада_12.08.2026.xls"))
        assertEquals("volynpharm", ExcelParser.supplierPrefix("ВолиньФарм прайс 13.08.xls"))
        assertNull(ExcelParser.supplierPrefix("unknown_supplier.xls"))
    }

    @Test
    fun unknownSupplier_returnsNull() {
        val file = workbookFile("unknown_supplier.xls") { workbook ->
            val sheet = workbook.createSheet()
            sheet.createRow(0).createCell(0).setCellValue("anything")
        }
        assertNull(ExcelParser.parseFile(file))
    }

    @Test
    fun parseFile_usesExplicitParserWhenSupplied() {
        val file = workbookFile("unknown_supplier.xls") { workbook ->
            val sheet = workbook.createSheet()
            repeat(4) { sheet.createRow(it) }
            val rowA = sheet.createRow(4)
            rowA.createCell(1).setCellValue("Аспірин")
            rowA.createCell(3).setCellValue("Bayer")
            rowA.createCell(5).setCellValue(12.0)
            rowA.createCell(9).setCellValue("Без ПДВ")
            rowA.createCell(10).setCellValue("101")
        }

        val parsed = ExcelParser.parseFile(file, "dekada")
        assertNotNull(parsed)
        assertEquals("Декада", parsed!!.supplier)
        assertEquals(1, parsed.rows.size)
        assertEquals("Аспірин", parsed.rows[0].article)
    }

    @Test
    fun parseFile_guessesParserFromContentWhenFileNameUnknown() {
        val file = workbookFile("price.xls") { workbook ->
            val sheet = workbook.createSheet()
            val rowA = sheet.createRow(1)
            rowA.createCell(0).setCellValue("Аспірин")
            rowA.createCell(1).setCellValue("Bayer")
            rowA.createCell(3).setCellValue(12.0)
            rowA.createCell(5).setCellValue("101")
            rowA.createCell(6).setCellValue("Y")
        }

        val parsed = ExcelParser.parseFile(file)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.rows.size)
        assertEquals("Аспірин", parsed.rows[0].article)
    }

    @Test
    fun parserOptions_containsAllSuppliers() {
        val options = ExcelParser.parserOptions()
        assertEquals(10, options.size)
        assertTrue(options.any { it.first == "dekada" })
        assertTrue(options.all { (prefix, label) -> prefix.isNotBlank() && label.isNotBlank() })
    }

    @Test
    fun dekada_parsesRowsAndVatAndRounding() {
        val file = workbookFile("dekada_12-08-2026.xls") { workbook ->
            val sheet = workbook.createSheet()
            repeat(4) { sheet.createRow(it) }
            val headerRow = sheet.createRow(1)
            headerRow.createCell(1).setCellValue("header")

            val rowA = sheet.createRow(4)
            rowA.createCell(1).setCellValue("Аспірин")
            rowA.createCell(3).setCellValue("Bayer")
            rowA.createCell(5).setCellValue(12.345)
            rowA.createCell(9).setCellValue("Без ПДВ")
            rowA.createCell(10).setCellValue("101")

            val rowB = sheet.createRow(5)
            rowB.createCell(1).setCellValue("Ібупрофен")
            rowB.createCell(3).setCellValue("Галичфарм")
            rowB.createCell(5).setCellValue(44.5)
            rowB.createCell(9).setCellValue("З ПДВ")
            rowB.createCell(10).setCellValue("102")
        }

        val parsed = ExcelParser.parseFile(file)
        assertNotNull(parsed)
        val result = parsed!!
        assertEquals("Декада", result.supplier)
        assertTrue(result.dateAsString.matches(Regex("\\d{2}-\\d{2}-\\d{4}")))
        assertEquals(2, result.rows.size)

        val rowA = result.rows[0]
        assertEquals("Аспірин", rowA.article)
        assertEquals("Bayer", rowA.vendor)
        assertEquals(12.34, rowA.price!!, 0.001)
        assertEquals("Ні", rowA.vat)
        assertEquals("101", rowA.code)
        assertEquals("Декада ${result.dateAsString}", rowA.counteragent)

        val rowB = result.rows[1]
        assertEquals("Ібупрофен", rowB.article)
        assertEquals(44.50, rowB.price!!, 0.001)
        assertEquals("Так", rowB.vat)
    }

    @Test
    fun dekada_stopsAtEmptyArticleColumn() {
        val file = workbookFile("dekada_13-08-2026.xls") { workbook ->
            val sheet = workbook.createSheet()
            repeat(4) { sheet.createRow(it) }
            val rowA = sheet.createRow(4)
            rowA.createCell(1).setCellValue("Аспірин")
            rowA.createCell(3).setCellValue("Bayer")
            rowA.createCell(5).setCellValue(10.0)
            rowA.createCell(9).setCellValue("Без ПДВ")
            rowA.createCell(10).setCellValue("101")

            val rowB = sheet.createRow(5)
            rowB.createCell(5).setCellValue(99.0)

            val rowC = sheet.createRow(6)
            rowC.createCell(1).setCellValue("Ібупрофен")
            rowC.createCell(3).setCellValue("Галичфарм")
            rowC.createCell(5).setCellValue(20.0)
            rowC.createCell(9).setCellValue("З ПДВ")
            rowC.createCell(10).setCellValue("103")
        }

        val parsed = ExcelParser.parseFile(file)
        assertNotNull(parsed)
        assertEquals(1, parsed!!.rows.size)
        assertEquals("Аспірин", parsed.rows[0].article)
    }

    private fun workbookFile(fileName: String, populate: (HSSFWorkbook) -> Unit): File {
        val dir = File(System.getProperty("java.io.tmpdir") ?: ".")
        val file = File(dir, fileName)
        HSSFWorkbook().use { workbook ->
            populate(workbook)
            dir.mkdirs()
            file.outputStream().use { workbook.write(it) }
        }
        return file
    }
}
