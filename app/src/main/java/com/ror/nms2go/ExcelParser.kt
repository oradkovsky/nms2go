package com.ror.nms2go

import org.apache.poi.hssf.OldExcelFormatException
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExcelRow(
    val counteragent: String,
    val article: String,
    val vendor: String,
    val price: Double?,
    val vat: String,
    val code: String,
    val receiver: String = "",
    val company: String = ""
)

data class ParsedExcel(
    val supplier: String,
    val dateAsString: String,
    val rows: List<ExcelRow>
)

object ExcelParser {
    private val formatter = DataFormatter()

    private val suppliers = listOf(
        "alba" to "альба",
        "badm" to "бадм",
        "venta" to "вента",
        "dekada" to "декада",
        "fram" to "фрам",
        "konex" to "конекс",
        "pharmplanet" to "фармпланета",
        "volynpharm" to "волиньфарм",
        "ametrin" to "аметрін",
        "fitolek" to "фітолек"
    )

    fun parserOptions(): List<Pair<String, String>> {
        return suppliers.map { (prefix, _) -> prefix to (supplierLabel(prefix) ?: prefix) }
    }

    fun supplierPrefix(fileName: String): String? {
        val lowered = fileName.lowercase()
        val beforeUnderscore = lowered.substringBefore('_')
        for ((prefix, _) in suppliers) {
            if (beforeUnderscore == prefix) return prefix
        }
        for ((prefix, label) in suppliers) {
            if (lowered.contains(prefix) || lowered.contains(label)) return prefix
        }
        return null
    }

    fun parseFile(
        file: File,
        parserPrefix: String? = null,
        dateAsString: String? = null
    ): ParsedExcel? {
        val prefix = parserPrefix?.takeIf { it.isNotBlank() } ?: supplierPrefix(file.name)
        return parseWithPrefix(file, prefix, dateAsString) ?: guessParser(file, dateAsString)
    }

    private fun parseWithPrefix(file: File, prefix: String?, dateAsStringOverride: String? = null): ParsedExcel? {
        if (prefix == null) return null
        val supplier = supplierLabel(prefix) ?: return null
        val dateAsString = dateAsStringOverride
            ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(file.lastModified()))
        return parseWithAccess(readSheet(file), prefix, supplier, dateAsString)
    }

    private fun parseWithAccess(
        access: SheetAccess,
        prefix: String,
        supplier: String,
        dateAsString: String
    ): ParsedExcel {
        val rows = when (prefix) {
            "alba" -> alba(access, supplier, dateAsString)
            "badm" -> badm(access, supplier, dateAsString)
            "venta" -> venta(access, supplier, dateAsString)
            "dekada" -> dekada(access, supplier, dateAsString)
            "fram" -> fram(access, supplier, dateAsString)
            "konex" -> konex(access, supplier, dateAsString)
            "pharmplanet" -> pharmplanet(access, supplier, dateAsString)
            "volynpharm" -> volynpharm(access, supplier, dateAsString)
            "ametrin" -> ametrin(access, supplier, dateAsString)
            "fitolek" -> fitolek(access, supplier, dateAsString)
            else -> emptyList()
        }
        return ParsedExcel(supplier, dateAsString, rows)
    }

    private fun guessParser(file: File, dateAsStringOverride: String? = null): ParsedExcel? {
        val access = readSheet(file)
        val dateAsString = dateAsStringOverride
            ?: SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(file.lastModified()))
        return parserOptions()
            .mapNotNull { (prefix, _) ->
                val supplier = supplierLabel(prefix) ?: return@mapNotNull null
                parseWithAccess(access, prefix, supplier, dateAsString)
            }
            .maxByOrNull { it.rows.size }
            ?.takeIf { it.rows.isNotEmpty() }
    }

    private fun readSheet(file: File): SheetAccess {
        return try {
            val workbook = WorkbookFactory.create(file)
            val sheet = workbook.getSheetAt(0)
            val grid = sheet.lastRowNum.takeIf { it >= 0 }?.let { last ->
                Grid(
                    rows = (0..last).map { row ->
                        val sheetRow = sheet.getRow(row)
                        RowData(
                            text = (0..maxOf(sheetRow?.lastCellNum?.toInt() ?: 0, 0)).map { col ->
                                val cell = sheetRow?.getCell(col) ?: return@map null
                                runCatching { formatter.formatCellValue(cell).trim() }
                                    .getOrNull()
                                    ?.takeIf { it.isNotEmpty() }
                            },
                            decimals = (0..maxOf(sheetRow?.lastCellNum?.toInt() ?: 0, 0)).map { col ->
                                val cell = sheetRow?.getCell(col) ?: return@map null
                                when (cell.cellType) {
                                    CellType.NUMERIC -> cell.numericCellValue
                                    CellType.STRING -> cell.stringCellValue.trim().replace(',', '.').toDoubleOrNull()
                                    CellType.FORMULA -> runCatching { cell.numericCellValue }.getOrNull()
                                    else -> null
                                }
                            }
                        )
                    }
                )
            } ?: Grid(emptyList())
            workbook.close()
            GridSheetAccess(grid)
        } catch (e: OldExcelFormatException) {
            // Excel 5.0/7.0 (BIFF5) files are not supported by POI; use JExcelAPI.
            val settings = jxl.WorkbookSettings().apply {
                encoding = "Cp1251"
            }
            val workbook = jxl.Workbook.getWorkbook(file, settings)
            val sheet = workbook.getSheet(0)
            val grid = if (sheet.rows > 0) {
                Grid(
                    rows = (0 until sheet.rows).map { row ->
                        RowData(
                            text = (0 until sheet.columns).map { col ->
                                sheet.getCell(col, row).contents.trim().takeIf { it.isNotEmpty() }
                            },
                            decimals = (0 until sheet.columns).map { col ->
                                val cell = sheet.getCell(col, row)
                                when (cell.type) {
                                    jxl.CellType.NUMBER, jxl.CellType.NUMBER_FORMULA ->
                                        runCatching { (cell as jxl.NumberCell).value }.getOrNull()
                                    else -> cell.contents.trim().replace(',', '.').toDoubleOrNull()
                                }
                            }
                        )
                    }
                )
            } else {
                Grid(emptyList())
            }
            workbook.close()
            GridSheetAccess(grid)
        }
    }

    private data class RowData(val text: List<String?>, val decimals: List<Double?>)

    private data class Grid(val rows: List<RowData>)

    private class GridSheetAccess(private val grid: Grid) : SheetAccess {
        override val lastRowNum: Int get() = grid.rows.lastIndex

        override fun text(row: Int, index: Int): String? {
            return grid.rows.getOrNull(row)?.text?.getOrNull(index)
        }

        override fun decimal(row: Int, index: Int): Double? {
            return grid.rows.getOrNull(row)?.decimals?.getOrNull(index)
        }
    }

    private interface SheetAccess {
        val lastRowNum: Int
        fun text(row: Int, index: Int): String?
        fun decimal(row: Int, index: Int): Double?
    }

    private fun supplierLabel(prefix: String): String? = when (prefix) {
        "alba" -> "Альба"
        "badm" -> "Бадм"
        "venta" -> "Вента"
        "dekada" -> "Декада"
        "fram" -> "Фрам"
        "konex" -> "Конекс"
        "pharmplanet" -> "Фармпланета"
        "volynpharm" -> "ВолиньФарм"
        "ametrin" -> "Аметрін"
        "fitolek" -> "Фітолек"
        else -> null
    }

    private fun alba(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        for (i in 1..access.lastRowNum) {
            val article = access.text(i, 0) ?: continue
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 1).orEmpty(),
                price = access.decimal(i, 3),
                vat = if (access.text(i, 6) == "Y") "Так" else "Ні",
                code = access.text(i, 5).orEmpty()
            )
        }
        return rows
    }

    // Mirrors BadmExcelConverter.Innerworkings (C#):
    //  Rows[i][2] -> C_NAME, Rows[i][3] -> C_MANUFACTURE, Rows[i][4] -> C_PRICE,
    //  Rows[i][9] == "БЕЗ НДС" ? "Ні" : "Так" -> C_VAT_FLAG, Rows[i][1] -> C_CODE (не моріоновський)
    //  i starts at 4, supplier = "Бадм " + dateAsString
    private fun badm(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 4
        while (i <= access.lastRowNum) {
            val article = access.text(i, 2) // C#: result.Tables[0].Rows[i][2]
            if (article == null) {
                i++
                continue
            }
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString", // C#: "Бадм " + dateAsString
                article = article,
                vendor = access.text(i, 3).orEmpty(), // C#: Rows[i][3]
                price = access.decimal(i, 4), // C#: Rows[i][4]
                vat = if (access.text(i, 9) == "БЕЗ НДС") "Ні" else "Так", // C#: Rows[i][9] == "БЕЗ НДС" ? "Ні" : "Так"
                code = access.text(i, 1).orEmpty() // C#: Rows[i][1] // не моріоновський
            )
            i++
        }
        return rows
    }

    private fun venta(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 3
        while (i <= access.lastRowNum) {
            val article = access.text(i, 1) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 8).orEmpty(),
                price = access.decimal(i, 4)?.round2(),
                vat = if (access.text(i, 10) == "0") "Ні" else "Так",
                code = access.text(i, 12).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun dekada(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 4
        while (i <= access.lastRowNum) {
            val article = access.text(i, 1) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 3).orEmpty(),
                price = access.decimal(i, 5)?.round2(),
                vat = if (access.text(i, 9) == "Без ПДВ") "Ні" else "Так",
                code = access.text(i, 10).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun fram(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 15
        while (i <= access.lastRowNum) {
            val article = access.text(i, 1) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 7).orEmpty(),
                price = access.decimal(i, 6),
                vat = if (access.text(i, 9) == "НДС") "Так" else "Ні",
                code = access.text(i, 3).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun konex(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 4
        while (i <= access.lastRowNum) {
            val article = access.text(i, 1) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 2).orEmpty(),
                price = access.decimal(i, 3),
                vat = access.text(i, 6).orEmpty(),
                code = access.text(i, 8).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun pharmplanet(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 1
        while (i <= access.lastRowNum) {
            val article = access.text(i, 1) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 2).orEmpty(),
                price = access.decimal(i, 6),
                vat = access.text(i, 3).orEmpty(),
                code = access.text(i, 0).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun volynpharm(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 1
        while (i <= access.lastRowNum) {
            val article = access.text(i, 2) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 3).orEmpty(),
                price = access.decimal(i, 6),
                vat = access.text(i, 4).orEmpty(),
                code = access.text(i, 1).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun ametrin(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val start = if (access.text(0, 0).isNullOrEmpty()) 13 else 1
        val rows = mutableListOf<ExcelRow>()
        var i = start
        while (i <= access.lastRowNum) {
            val code = access.text(i, 0) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = access.text(i, 2).orEmpty(),
                vendor = access.text(i, 3).orEmpty(),
                price = access.decimal(i, 9),
                vat = access.text(i, 10).orEmpty(),
                code = code
            )
            i++
        }
        return rows
    }

    private fun fitolek(access: SheetAccess, supplier: String, dateAsString: String): List<ExcelRow> {
        val rows = mutableListOf<ExcelRow>()
        var i = 7
        while (i <= access.lastRowNum) {
            val article = access.text(i, 2) ?: break
            if (article == "Наименование") break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 4).orEmpty(),
                price = access.decimal(i, 8),
                vat = "Ні",
                code = access.text(i, 1).orEmpty()
            )
            i++
        }

        i += 2
        while (i <= access.lastRowNum) {
            val article = access.text(i, 2) ?: break
            rows += ExcelRow(
                counteragent = "$supplier $dateAsString",
                article = article,
                vendor = access.text(i, 4).orEmpty(),
                price = access.decimal(i, 8),
                vat = "Так",
                code = access.text(i, 1).orEmpty()
            )
            i++
        }
        return rows
    }

    private fun Double.round2(): Double =
        BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_EVEN).toDouble()
}
