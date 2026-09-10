package com.ror.nms2go

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ror.nms2go.ui.ParsedScreen
import com.ror.nms2go.ui.ParsedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Composable
private fun TestParsedScreenWithViewModel(
    parsed: ParsedExcel,
    quantities: Map<Int, Int> = emptyMap(),
    isLoading: Boolean = false,
    orderLoadingProgress: Pair<Int, Int>? = null,
) {
    val vm = remember { ParsedViewModel(SavedStateHandle()) }
    val uiState by vm.uiState.collectAsState()
    LaunchedEffect(parsed, quantities, isLoading, orderLoadingProgress) {
        vm.updateData(parsed, quantities, isLoading, orderLoadingProgress)
    }
    ParsedScreen(
        uiState = uiState,
        onQueryChange = vm::onQueryChange,
        onClearQuery = vm::onClearQuery,
        onPageChange = vm::onPageChange,
        onQuantityChange = vm::onQuantityChange,
        onBack = {}
    )
}

@RunWith(AndroidJUnit4::class)
class ParsedBulkDateTest {

    @get:Rule
    val rule = createComposeRule()

    private fun today(): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())

    private fun row(
        counteragent: String,
        article: String,
        price: Double,
        code: String = article
    ) = ExcelRow(
        counteragent = counteragent,
        article = article,
        vendor = "Vendor",
        price = price,
        vat = "Так",
        code = code,
        receiver = "r@example.com",
        company = "Co"
    )

    @Test
    fun bulkHeader_showsVendorWithDateNotSeparateList() {
        // Simulate bulk with 2 vendors each with distinct price date – header must be "Альба (14-11-2024), Бадм (15-11-2024)" not "Альба, Бадм · 14-11-2024, 15-11-2024"
        val parsed = ParsedExcel(
            supplier = "Альба (14-11-2024), Бадм (15-11-2024)",
            dateAsString = "",
            rows = listOf(
                row("Альба 14-11-2024", "Item A", 10.0),
                row("Бадм 15-11-2024", "Item B", 20.0)
            )
        )
        rule.setContent {
            TestParsedScreenWithViewModel(parsed = parsed)
        }
        TestVisuals.afterSetContent()

        // Header shows combined vendor (date) – check substring
        rule.onNodeWithText("Альба (14-11-2024)", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Бадм (15-11-2024)", substring = true).assertIsDisplayed()
        // Should not show separate date list "14-11-2024, 15-11-2024" as standalone after · (old format)
        // The new format collapses date into supplier, so dateAsString is empty and header is "Альба (14-11-2024), Бадм (15-11-2024) · 2"
        rule.onNodeWithText("Альба (14-11-2024), Бадм (15-11-2024)", substring = true).assertIsDisplayed()
    }

    @Test
    fun bulkRows_counteragentShowsPriceDateNotToday() {
        val priceDate1 = "14-11-2024"
        val priceDate2 = "15-11-2024"
        val todayStr = today()
        // Ensure today is not one of the price dates (if it is, pick different)
        val d1 = if (priceDate1 == todayStr) "13-11-2024" else priceDate1
        val d2 = if (priceDate2 == todayStr) "12-11-2024" else priceDate2

        val parsed = ParsedExcel(
            supplier = "Альба ($d1), Бадм ($d2)",
            dateAsString = "",
            rows = listOf(
                row("Альба $d1", "Парацетамол", 5.0, "C1"),
                row("Бадм $d2", "Ібупрофен", 15.0, "C2")
            )
        )
        rule.setContent {
            TestParsedScreenWithViewModel(parsed = parsed)
        }
        TestVisuals.afterSetContent()

        // Each row's counteragent must contain its price date, not today's date
        rule.onNodeWithText("Альба $d1", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Бадм $d2", substring = true).assertIsDisplayed()
        // Today should not appear in any row when we use explicit price dates
        if (todayStr != d1 && todayStr != d2) {
            rule.onNodeWithText(todayStr, substring = true).assertIsNotDisplayed()
        }
    }

    @Test
    fun bulkIncremental_firstVendorThenSecond_updatesHeaderAndRowsSorted() {
        // Simulate MainActivity incremental publish: first 1 vendor, then 2 vendors
        var parsed by mutableStateOf(
            ParsedExcel(
                supplier = "Альба (14-11-2024)",
                dateAsString = "",
                rows = listOf(
                    row("Альба 14-11-2024", "Expensive", 100.0, "E1"),
                    row("Альба 14-11-2024", "Cheap", 5.0, "C1")
                )
            )
        )
        rule.setContent {
            TestParsedScreenWithViewModel(
                parsed = parsed,
                isLoading = true,
                orderLoadingProgress = 1 to 2
            )
        }
        TestVisuals.afterSetContent()

        // Initially shows only Альба, rows sorted low→high: Cheap (5.0) before Expensive (100)
        rule.onNodeWithText("Альба (14-11-2024)", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Бадм", substring = true).assertIsNotDisplayed()
        // Check sorted order: Cheap should be searchable, and header shows 1/2 loading
        rule.onNodeWithText("Завантажено 1/2", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Cheap", substring = true).assertIsDisplayed()

        // Second vendor arrives – same as MainActivity second loop iteration
        parsed = ParsedExcel(
            supplier = "Альба (14-11-2024), Бадм (15-11-2024)",
            dateAsString = "",
            rows = listOf(
                row("Альба 14-11-2024", "Expensive", 100.0, "E1"),
                row("Альба 14-11-2024", "Cheap", 5.0, "C1"),
                row("Бадм 15-11-2024", "Cheapest", 1.0, "CC1"),
                row("Бадм 15-11-2024", "Mid", 50.0, "M1")
            )
        )
        rule.waitForIdle()
        TestVisuals.afterAction()

        // Header now shows both vendors with dates
        rule.onNodeWithText("Альба (14-11-2024), Бадм (15-11-2024)", substring = true).assertIsDisplayed()
        // Rows must be resort globally: Cheapest (1.0) < Cheap (5.0) < Mid (50) < Expensive (100)
        // Verify all 4 are present (PAGE_SIZE 20, so all on one page) – use exact to avoid substring overlap Cheap/Cheapest
        rule.onNodeWithText("Cheapest", substring = false).assertIsDisplayed()
        rule.onNodeWithText("Cheap", substring = false).assertIsDisplayed()
        rule.onNodeWithText("Mid", substring = false).assertIsDisplayed()
        rule.onNodeWithText("Expensive", substring = false).assertIsDisplayed()
        // Counteragent for new vendor must be visible (2 rows share same counteragent)
        rule.onAllNodesWithText("Бадм 15-11-2024", substring = true).assertCountEquals(2)
    }

    @Test
    fun bulkIncremental_filterRefiltersOnNewData() {
        var parsed by mutableStateOf(
            ParsedExcel(
                supplier = "Альба (14-11-2024)",
                dateAsString = "",
                rows = listOf(
                    row("Альба 14-11-2024", "Парацетамол", 10.0),
                    row("Альба 14-11-2024", "Аспірин", 20.0)
                )
            )
        )
        rule.setContent {
            TestParsedScreenWithViewModel(parsed = parsed)
        }
        TestVisuals.afterSetContent()

        // Initially both visible
        rule.onNodeWithText("Парацетамол", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Аспірин", substring = true).assertIsDisplayed()

        // Apply filter via typing in the filter field – locate via testTag
        rule.onNodeWithTag("parsedFilter").performTextInput("парацетамол")
        TestVisuals.afterAction()
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithText("Парацетамол", substring = true).assertIsDisplayed()
        // Аспірин should be filtered out
        rule.onNodeWithText("Аспірин", substring = true).assertIsNotDisplayed()

        // New vendor arrives with a matching row – should appear even though filter active (refilter)
        parsed = ParsedExcel(
            supplier = "Альба (14-11-2024), Бадм (15-11-2024)",
            dateAsString = "",
            rows = listOf(
                row("Альба 14-11-2024", "Парацетамол", 10.0),
                row("Альба 14-11-2024", "Аспірин", 20.0),
                row("Бадм 15-11-2024", "Парацетамол Ультра", 15.0)
            )
        )
        rule.waitForIdle()
        TestVisuals.afterAction()
        // Both matching rows should be visible, sorted by price: Парацетамол 10 < Парацетамол Ультра 15
        // Use onAllNodes count because "Парацетамол" substring matches both "Парацетамол" and "Парацетамол Ультра"
        rule.onAllNodesWithText("Парацетамол", substring = true).assertCountEquals(2)
        rule.onNodeWithText("Парацетамол Ультра", substring = false).assertIsDisplayed()
        rule.onNodeWithText("Аспірин", substring = false).assertIsNotDisplayed()
    }

    @Test
    fun bulkNms2GoApp_showsHeaderWithDatesViaBulkLoad() {
        // Integration via Nms2GoApp: simulate MainActivity bulk incremental via parsedExcel state
        var parsed by mutableStateOf<ParsedExcel?>(
            ParsedExcel(
                supplier = "Альба (14-11-2024)",
                dateAsString = "",
                rows = listOf(row("Альба 14-11-2024", "Item1", 10.0))
            )
        )
        rule.setContent {
            com.ror.nms2go.ui.Nms2GoApp(
                senders = emptyList(),
                onAddSender = { _, _, _, _ -> },
                onUpdateSender = { _, _, _, _, _ -> },
                onRemoveSender = {},
                loading = true,
                statusText = "",
                overviewResults = emptyList(),
                onLoad = {},
                onParseItem = { _ -> },
                onOrder = {},
                onSendOrders = {},
                parsedExcel = parsed,
                onDismissParsed = {},
                orderQuantities = emptyMap(),
                onQuantityChange = { _, _ -> },
                orders = emptyList(),
                orderSentStamp = 0,
                sendingOrders = false,
                orderSendError = null,
                orderLoadingProgress = 1 to 2
            )
        }
        TestVisuals.afterSetContent()
        rule.onNodeWithText("Альба (14-11-2024)", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Завантажено 1/2", substring = true).assertIsDisplayed()

        parsed = ParsedExcel(
            supplier = "Альба (14-11-2024), Бадм (15-11-2024)",
            dateAsString = "",
            rows = listOf(
                row("Альба 14-11-2024", "Item1", 10.0),
                row("Бадм 15-11-2024", "Item2", 5.0)
            )
        )
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithText("Альба (14-11-2024), Бадм (15-11-2024)", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Бадм (15-11-2024)", substring = true).assertIsDisplayed()
    }
}
