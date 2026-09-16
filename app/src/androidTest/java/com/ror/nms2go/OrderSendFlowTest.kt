package com.ror.nms2go

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.ror.nms2go.data.AppDatabase
import com.ror.nms2go.data.OrderDao
import com.ror.nms2go.data.OrderItemEntity
import com.ror.nms2go.data.OrderStatus
import com.ror.nms2go.data.SentOrderEntity
import com.ror.nms2go.data.SentOrderWithItems
import com.ror.nms2go.ui.Nms2GoApp
import com.ror.nms2go.ui.ORDER_BUTTON_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OrderSendFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var orderDao: OrderDao

    @Inject
    lateinit var appDatabase: AppDatabase

    @Before
    fun init() {
        hiltRule.inject()
        runBlocking { appDatabase.clearAllTables() }
    }

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun createTestOrder(id: Long = 1, company: String = "Test Supplier"): SentOrderWithItems =
        SentOrderWithItems(
            order = SentOrderEntity(
                id = id,
                sentAt = 1_700_000_000_000L,
                company = company,
                senderEmail = "sender@example.com",
                receiverEmail = "receiver@example.com",
                subject = "Order $company 2024-11-14 10:00",
                status = OrderStatus.SENT,
                error = null
            ),
            items = listOf(
                OrderItemEntity(id = 10, orderId = id, code = "C1", name = "Парацетамол", price = 12.5, quantity = 1)
            )
        )

    private class SendFlowHarness(
        val stamp: MutableState<Int>,
        val sending: MutableState<Boolean>,
        val error: MutableState<String?>
    )

    private fun sendFlowContent(onSendOrders: () -> Unit): SendFlowHarness {
        val stamp = mutableIntStateOf(0)
        val sending = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        val parsed = mutableStateOf<ParsedExcel?>(null)
        rule.setContent {
            Nms2GoApp(
                senders = emptyList(),
                loading = false,
                statusText = "",
                overviewResults = emptyList(),
                onLoad = {},
                onParseItem = { _ -> },
                onOrder = {},
                onSendOrders = onSendOrders,
                parsedExcel = parsed.value,
                onDismissParsed = { parsed.value = null },
                orderQuantities = mapOf(0 to 2),
                onQuantityChange = { _, _ -> },
                orderSentStamp = stamp.value,
                sendingOrders = sending.value,
                orderSendError = error.value
            )
        }
        TestVisuals.afterSetContent()
        rule.waitForIdle()
        parsed.value = ParsedExcel(
            supplier = "Test Supplier",
            dateAsString = "01-01-2024",
            rows = listOf(
                ExcelRow(
                    counteragent = "Test Co",
                    article = "Item A",
                    vendor = "",
                    price = 100.0,
                    vat = "Так",
                    code = "Item A | Test Co",
                    receiver = "receiver@example.com",
                    company = "Test Supplier"
                )
            )
        )
        rule.waitForIdle()
        TestVisuals.afterSetContent()
        return SendFlowHarness(stamp, sending, error)
    }

    @Test
    fun orderSend_success_showsSendingThenLandsOnOrders() {
        var sendCalls = 0
        val harness = sendFlowContent { sendCalls++ }

        rule.onNodeWithText(appContext.getString(R.string.parsed_title)).assertIsDisplayed()

        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_title)).assertIsDisplayed()

        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsEnabled()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()
        assertEquals(1, sendCalls)

        harness.sending.value = true
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()

        harness.sending.value = false
        harness.error.value = null
        harness.stamp.value = harness.stamp.value + 1
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onAllNodesWithTag(ORDER_BUTTON_TAG).assertCountEquals(0)
        // Drawer + TopBar both show orders_title after rename, so 2 nodes in tree (one hidden in drawer)
        rule.onAllNodesWithText(appContext.getString(R.string.orders_title)).assertCountEquals(2)
        rule.onNodeWithText(appContext.getString(R.string.orders_empty)).assertIsDisplayed()
    }

    @Test
    fun orderSend_failure_showsErrorAndAllowsRetry() {
        var sendCalls = 0
        val harness = sendFlowContent { sendCalls++ }

        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_title)).assertIsDisplayed()

        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()
        assertEquals(1, sendCalls)

        harness.sending.value = true
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()

        val failure = "Could not send orders: simulated failure"
        harness.sending.value = false
        harness.error.value = failure
        rule.waitForIdle()
        TestVisuals.afterAction()

        // Current production shows Error state (hides orderButton); retry requires clearing error
        rule.onNodeWithText(failure).assertIsDisplayed()
        rule.onAllNodesWithTag(ORDER_BUTTON_TAG).assertCountEquals(0)
        rule.onNodeWithText(appContext.getString(R.string.back)).assertIsDisplayed()
        // Simulate error cleared (as if user dismissed) - Content should return with retry
        harness.error.value = null
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsEnabled()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()
        assertEquals(2, sendCalls)

        harness.sending.value = true
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
    }

    @Test
    fun overview_autoReloadsWhenReturnedToAfterSend() {
        var loadCalls = 0
        val stamp = mutableIntStateOf(0)
        val parsed = mutableStateOf<ParsedExcel?>(null)
        val ordersState = mutableStateOf<List<SentOrderWithItems>>(emptyList())
        val overviewResultsState = mutableStateOf<List<com.ror.nms2go.data.SenderOverview>>(emptyList())
        val sender = com.ror.nms2go.data.SenderEntity(
            id = 1,
            companyName = "Test Co",
            email = "sender@example.com"
        )
        val parsedExcel = ParsedExcel(
            supplier = "Test Supplier",
            dateAsString = "01-01-2024",
            rows = listOf(
                ExcelRow(
                    counteragent = "Test Co",
                    article = "Item A",
                    vendor = "",
                    price = 100.0,
                    vat = "Так",
                    code = "Item A | Test Co",
                    receiver = "receiver@example.com",
                    company = "Test Supplier"
                )
            )
        )
        fun createOverview(): com.ror.nms2go.data.SenderOverview =
            com.ror.nms2go.data.SenderOverview(
                senderQuery = sender.email,
                messageId = "msg1",
                subject = "Price list 01-01-2024",
                from = sender.email,
                date = "01.01.2024 10:00",
                hasAttachment = true,
                status = com.ror.nms2go.data.SenderOverview.Status.FOUND,
                attachment = null
            )
        rule.setContent {
            Nms2GoApp(
                senders = listOf(sender),
                loading = false,
                statusText = "",
                overviewResults = overviewResultsState.value,
                onLoad = {
                    loadCalls++
                    // Simulate Gmail load returning at least one FOUND item per configured sender
                    // N = senders.size (here 1) – test with 1 vendor, but supports N vendors
                    overviewResultsState.value = listOf(createOverview())
                },
                onParseItem = { _ -> },
                onOrder = {},
                onSendOrders = {
                    stamp.value = stamp.value + 1
                    val order = createTestOrder(company = "Test Supplier")
                    runBlocking {
                        val orderId = orderDao.insertOrder(order.order.copy(id = 0))
                        orderDao.insertOrderItems(order.items.map { it.copy(id = 0, orderId = orderId) })
                    }
                    ordersState.value = listOf(order)
                },
                parsedExcel = parsed.value,
                onDismissParsed = { parsed.value = null },
                orderQuantities = mapOf(0 to 2),
                onQuantityChange = { _, _ -> },
                orderSentStamp = stamp.value,
                sendingOrders = false,
                orderSendError = null
            )
        }
        TestVisuals.afterSetContent()

        rule.waitForIdle()
        TestVisuals.afterAction()
        val callsAfterStart = loadCalls

        parsed.value = parsedExcel
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()
        rule.onAllNodesWithText(appContext.getString(R.string.orders_title)).assertCountEquals(2)
        // Verify order is correctly reflected on the list after bulk send
        rule.onNodeWithText("Test Supplier").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.orders_count, 1)).assertIsDisplayed()
        rule.onNodeWithText("receiver@example.com", substring = true).assertIsDisplayed()

        // Simulate that overview needs to reload after order – clear current results so LaunchedEffect triggers on return
        overviewResultsState.value = emptyList()
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithContentDescription(appContext.getString(R.string.menu_open)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.menu_overview)).performClick()
        TestVisuals.afterAction()
        rule.waitForIdle()
        TestVisuals.afterAction()

        // After returning to OVERVIEW, only drawer (hidden) contains orders_title, so 1 node in tree, none displayed in TopBar
        rule.onAllNodesWithText(appContext.getString(R.string.orders_title))
            .assertCountEquals(1)
        rule.onNodeWithText(appContext.getString(R.string.overview_intro)).assertIsDisplayed()
        // Overview must contain at least one item (N= senders.size) after reload, not blank
        rule.onNodeWithText("Test Co", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Price list", substring = true).assertIsDisplayed()
        assertEquals(callsAfterStart + 1, loadCalls)
        // Navigate back to Orders to ensure order still correctly reflected
        rule.onNodeWithContentDescription(appContext.getString(R.string.menu_open)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.orders_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText("Test Supplier").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.orders_count, 1)).assertIsDisplayed()
    }
}
