package com.ror.nms2go

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.ror.nms2go.data.AppDatabase
import com.ror.nms2go.data.GmailAuthManager
import com.ror.nms2go.ui.Nms2GoApp
import com.ror.nms2go.ui.ORDER_BUTTON_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var fakeGmail: FakeGmailSender

    @Inject
    lateinit var authManager: GmailAuthManager

    @Before
    fun init() {
        hiltRule.inject()
        FakeSenders.reset()
        fakeGmail.reset()
        runBlocking { appDatabase.clearAllTables() }
    }

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun sendFlowContent() {
        val parsed = mutableStateOf<ParsedExcel?>(null)
        rule.setContent {
            Nms2GoApp(
                loading = false,
                statusText = "",
                overviewResults = emptyList(),
                onLoad = {},
                onParseItem = { _ -> },
                onOrder = {},
                parsedExcel = parsed.value,
                onDismissParsed = { parsed.value = null },
                orderQuantities = mapOf(0 to 2),
                onQuantityChange = { _, _ -> }
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
    }

    @Test
    fun orderSend_success_showsSendingThenLandsOnOrders() {
        sendFlowContent()

        rule.onNodeWithText(appContext.getString(R.string.parsed_title)).assertIsDisplayed()

        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_title)).assertIsDisplayed()

        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsEnabled()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()

        // The Review screen owns its sending state: confirming switches the
        // order button to "Sending…" until auth is approved and delivery completes.
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()
        assertEquals(0, fakeGmail.sentMessages.size)

        authManager.onAuthorizationResult("fake-token")
        rule.waitForIdle()
        TestVisuals.afterAction()

        assertEquals(1, fakeGmail.sentMessages.size)
        assertEquals("receiver@example.com", fakeGmail.sentMessages.single().to)
        assertTrue(fakeGmail.sentMessages.single().subject.contains("Test Supplier"))

        rule.onAllNodesWithTag(ORDER_BUTTON_TAG).assertCountEquals(0)
        // Drawer + TopBar both show orders_title after rename, so 2 nodes in tree (one hidden in drawer)
        rule.onAllNodesWithText(appContext.getString(R.string.orders_title)).assertCountEquals(2)
        rule.onNodeWithText("Test Supplier").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.orders_count, 1)).assertIsDisplayed()
    }

    @Test
    fun orderSend_failure_showsError() {
        sendFlowContent()

        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_title)).assertIsDisplayed()

        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()

        val failure = "Could not send orders: simulated failure"
        fakeGmail.failWith = failure
        authManager.onAuthorizationResult("fake-token")
        rule.waitForIdle()
        TestVisuals.afterAction()

        // The Review screen owns the error: the order button is gone and the
        // back button offers the only way out.
        rule.onNodeWithText(failure, substring = true).assertIsDisplayed()
        rule.onAllNodesWithTag(ORDER_BUTTON_TAG).assertCountEquals(0)
        rule.onNodeWithText(appContext.getString(R.string.back)).assertIsDisplayed()
    }

    @Test
    fun orderSend_authFailure_showsError() {
        sendFlowContent()

        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()

        authManager.onAuthorizationFailure("auth boom")
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText("auth boom").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertDoesNotExist()
        rule.onNodeWithText(appContext.getString(R.string.back)).assertIsDisplayed()
    }

    @Test
    fun overview_autoReloadsWhenReturnedToAfterSend() {
        var loadCalls = 0
        val parsed = mutableStateOf<ParsedExcel?>(null)
        val overviewResultsState = mutableStateOf<List<com.ror.nms2go.data.SenderOverview>>(emptyList())
        val sender = com.ror.nms2go.data.SenderEntity(
            companyName = "Test Co",
            inboundEmail = "sender@example.com"
        )
        FakeSenders.senders.value = listOf(sender)
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
                senderQuery = sender.inboundEmail,
                messageId = "msg1",
                subject = "Price list 01-01-2024",
                from = sender.inboundEmail,
                date = "01.01.2024 10:00",
                hasAttachment = true,
                status = com.ror.nms2go.data.SenderOverview.Status.FOUND,
                attachment = null
            )
        rule.setContent {
            Nms2GoApp(
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
                parsedExcel = parsed.value,
                onDismissParsed = { parsed.value = null },
                orderQuantities = mapOf(0 to 2),
                onQuantityChange = { _, _ -> }
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
        authManager.onAuthorizationResult("fake-token")
        rule.waitForIdle()
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
