package com.ror.nms2go

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.ror.nms2go.data.OrderItemEntity
import com.ror.nms2go.data.OrderStatus
import com.ror.nms2go.data.SentOrderEntity
import com.ror.nms2go.data.SentOrderWithItems
import com.ror.nms2go.ui.Nms2GoApp
import com.ror.nms2go.ui.OrderDetailScreen
import com.ror.nms2go.ui.OrdersHistoryScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SentOrdersScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun formatTs(epochMillis: Long): String =
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(epochMillis))

    private fun order(
        id: Long = 1,
        company: String = "Альба",
        senderEmail: String = "alba@example.com",
        receiverEmail: String = "pharmacy@example.com",
        subject: String = "Order Альба 2024-11-14 10:00",
        status: String = OrderStatus.SENT,
        error: String? = null,
        items: List<OrderItemEntity> = listOf(
            OrderItemEntity(id = 10, orderId = id, code = "C1", name = "Парацетамол", price = 12.5, quantity = 3),
            OrderItemEntity(id = 11, orderId = id, code = "C2", name = "Ібупрофен", price = 95.0, quantity = 1)
        )
    ): SentOrderWithItems = SentOrderWithItems(
        order = SentOrderEntity(
            id = id,
            sentAt = 1_700_000_000_000L,
            company = company,
            senderEmail = senderEmail,
            receiverEmail = receiverEmail,
            subject = subject,
            status = status,
            error = error
        ),
        items = items
    )

    private fun setNms2GoApp(orders: List<SentOrderWithItems>) {
        rule.setContent {
            Nms2GoApp(
                senders = emptyList(),
                loading = false,
                statusText = "",
                overviewResults = emptyList(),
                onLoad = {},
                onParseItem = { _ -> },
                onOrder = {},
                onSendOrders = {},
                parsedExcel = null,
                onDismissParsed = {},
                orderQuantities = emptyMap(),
                onQuantityChange = { _, _ -> },
                orders = orders,
                orderSentStamp = 0,
                sendingOrders = false,
                orderSendError = null
            )
        }
        TestVisuals.afterSetContent()
    }

    @Test
    fun emptyOrdersHistory_showsEmptyState() {
        rule.setContent {
            OrdersHistoryScreen(orders = emptyList(), onOrderClick = {})
        }
        TestVisuals.afterSetContent()

        rule.onNodeWithText(appContext.getString(R.string.orders_empty)).assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.orders_empty_hint)).assertIsDisplayed()
    }

    @Test
    fun ordersHistory_showsOrderSummaryAndStatus() {
        // Merged T2+T3: 2 orders – one SENT (Альба), one FAILED (Вента) – covers both status branches
        val orders = listOf(
            order(id = 1, company = "Альба", receiverEmail = "pharmacy@example.com", status = OrderStatus.SENT),
            order(id = 3, company = "Вента", receiverEmail = "other.vent@example.com", status = OrderStatus.FAILED, error = "no receiver configured")
        )

        rule.setContent {
            OrdersHistoryScreen(orders = orders, onOrderClick = {})
        }
        TestVisuals.afterSetContent()

        rule.onNodeWithText(appContext.getString(R.string.orders_count, 2)).assertIsDisplayed()
        rule.onNodeWithText("Альба").assertIsDisplayed()
        rule.onNodeWithText("Вента").assertIsDisplayed()
        rule.onNodeWithText("pharmacy@example.com", substring = true).assertIsDisplayed()
        rule.onNodeWithText("other.vent@example.com", substring = true).assertIsDisplayed()
        // Both rows have 2 items / 4 units
        rule.onAllNodesWithText(
            appContext.getString(R.string.orders_row_items, 2, 4)
        ).assertCountEquals(2)
        rule.onNodeWithText(appContext.getString(R.string.orders_status_sent)).assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.orders_status_failed)).assertIsDisplayed()
        rule.onAllNodesWithText(formatTs(1_700_000_000_000L)).assertCountEquals(2)
    }

    @Test
    fun clickingOrderRow_invokesClickWithOrderId() {
        var clickedId = -1L
        rule.setContent {
            OrdersHistoryScreen(
                orders = listOf(order(id = 42, company = "Альба")),
                onOrderClick = { clickedId = it }
            )
        }
        TestVisuals.afterSetContent()

        rule.onNodeWithText("Альба").performClick()
        TestVisuals.afterAction()

        assertEquals(42L, clickedId)
    }

    @Test
    fun orderDetail_showsSenderReceiverSubjectAndItems() {
        // Merged T5+T6: verify happy detail plus error branch via state update (single setContent)
        val sentFailed = order(
            id = 4,
            company = "Вента",
            status = OrderStatus.FAILED,
            error = "no receiver configured"
        )
        val sentHappy = order()
        var currentOrders by mutableStateOf(listOf(sentHappy))
        var currentId by mutableStateOf(1L)

        rule.setContent {
            OrderDetailScreen(orders = currentOrders, orderId = currentId, onBack = {})
        }
        TestVisuals.afterSetContent()
        rule.onNodeWithText("Альба").assertIsDisplayed()
        rule.onNodeWithText(sentHappy.order.senderEmail).assertIsDisplayed()
        rule.onNodeWithText(sentHappy.order.receiverEmail).assertIsDisplayed()
        rule.onNodeWithText(sentHappy.order.subject).assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.order_detail_summary, 2, 4)).assertIsDisplayed()
        rule.onNodeWithText("Парацетамол").assertIsDisplayed()
        rule.onNodeWithText("×3").assertIsDisplayed()
        rule.onNodeWithText("Ібупрофен").assertIsDisplayed()
        rule.onNodeWithText("×1").assertIsDisplayed()

        // Switch to failed case – same screen but with error (state update, not second setContent)
        currentOrders = listOf(sentFailed)
        currentId = 4L
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.order_detail_error)).assertIsDisplayed()
        rule.onNodeWithText(sentFailed.order.error.orEmpty()).assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.orders_status_failed)).assertIsDisplayed()
        // Also verify common fields still present for failed
        rule.onNodeWithText("Вента").assertIsDisplayed()
    }

    @Test
    fun orderDetail_missingOrder_showsNotFoundMessage() {
        rule.setContent {
            OrderDetailScreen(orders = emptyList(), orderId = 99, onBack = {})
        }
        TestVisuals.afterSetContent()

        rule.onNodeWithText(appContext.getString(R.string.order_detail_missing))
            .assertIsDisplayed()
    }

    @Test
    fun fullNavigation_openingOrdersFromMenuThenRequestingDetails() {
        setNms2GoApp(
            orders = listOf(order(id = 1, company = "Альба", receiverEmail = "pharmacy@example.com"))
        )

        rule.onNodeWithContentDescription(appContext.getString(R.string.menu_open))
            .performClick()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.menu_orders)).performClick()
        TestVisuals.afterAction()

        // Slimmed: no longer asserts orders_count/Альба duplicate of T2, just navigation
        rule.onNodeWithText("Альба").performClick()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.order_detail_receiver))
            .assertIsDisplayed()
        rule.onNodeWithText("pharmacy@example.com").assertIsDisplayed()
    }
}
