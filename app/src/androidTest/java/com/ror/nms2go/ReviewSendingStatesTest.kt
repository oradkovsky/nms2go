package com.ror.nms2go

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ror.nms2go.ui.ORDER_BUTTON_TAG
import com.ror.nms2go.ui.ReviewScreen
import com.ror.nms2go.ui.ReviewViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Espresso coverage for the `_sending` states of [ReviewViewModel]:
 * a real ViewModel drives [ReviewScreen] and each combination of
 * `isSending` / `error` / `parsed` is asserted on the rendered UI.
 */
@HiltAndroidTest
class ReviewSendingStatesTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun row(article: String = "Item A") = ExcelRow(
        counteragent = "Test Co",
        article = article,
        vendor = "",
        price = 100.0,
        vat = "Так",
        code = "$article | Test Co",
        receiver = "receiver@example.com",
        company = "Test Supplier"
    )

    private fun parsed() = ParsedExcel(
        supplier = "Test Supplier",
        dateAsString = "01-01-2024",
        rows = listOf(row("Item A"), row("Item B"))
    )

    private fun setUp(viewModel: ReviewViewModel) {
        rule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            ReviewScreen(
                uiState = uiState,
                onQuantityChange = viewModel::onQuantityChange,
                onOrderRequested = viewModel::onOrderRequested,
                onConfirmOrder = viewModel::onConfirmOrder,
                onDismissConfirm = viewModel::onDismissConfirm,
                onBack = {}
            )
        }
        rule.waitForIdle()
    }

    @Test
    fun sendingTrue_disablesButtonAndShowsSending() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun sendingFalse_enablesButtonWithOrderText() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = false, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_order_button)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsEnabled()
    }

    @Test
    fun sendingTrueWithError_errorWinsAndHidesSendingIndicator() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = "boom")
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText("boom").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertDoesNotExist()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun sendingTrueWithNullParsed_showsEmpty() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(null, mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_empty)).assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertDoesNotExist()
    }

    @Test
    fun sendingTrueWithEmptyChosen_showsEmpty() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), emptyMap(), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_empty)).assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertDoesNotExist()
    }

    @Test
    fun quantitiesLocked_whileSending() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        // Stepper controls are disabled while sending.
        rule.onNodeWithContentDescription(appContext.getString(R.string.parsed_qty_add))
            .assertIsNotEnabled()
        rule.onNodeWithText("−").assertIsNotEnabled()

        // A quantity change attempted while sending is ignored – snapshot must not move.
        viewModel.onQuantityChange(1, 3)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText("2").assertIsDisplayed()
        rule.onNodeWithText("3").assertDoesNotExist()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun quantitiesEditable_whenNotSending() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = false, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithContentDescription(appContext.getString(R.string.parsed_qty_add))
            .assertIsEnabled()
        rule.onNodeWithText("−").assertIsEnabled()

        viewModel.onQuantityChange(1, 3)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText("3").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.review_order_button)).assertIsDisplayed()
    }

    @Test
    fun quantityTapBlocked_whileSending() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        // Central tap that opens the manual-entry dialog must do nothing while sending.
        rule.onNodeWithText("2").performClick()
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.quantity_dialog_title)).assertDoesNotExist()
        rule.onNodeWithText("2").assertIsDisplayed()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
    }

    @Test
    fun quantityTapOpensDialog_whenNotSending() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = false, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText("2").performClick()
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.quantity_dialog_title)).assertIsDisplayed()
    }

    @Test
    fun dialogBlocked_whileSending() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        // The order button is disabled with a "Sending…" label while sending,
        // so the confirm dialog must not be invocable.
        viewModel.onOrderRequested()
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_title)).assertDoesNotExist()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun dialogDismissed_whenSendingStarts() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = false, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        viewModel.onOrderRequested()
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_title)).assertIsDisplayed()

        // A stale dialog must not stay visible on top of the disabled "Sending…" button.
        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_title)).assertDoesNotExist()
        rule.onNodeWithText(appContext.getString(R.string.review_sending)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun sendingCleared_reenablesButton() {
        val viewModel = ReviewViewModel()
        setUp(viewModel)
        TestVisuals.afterSetContent()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = true, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsNotEnabled()

        viewModel.updateData(parsed(), mapOf(0 to 2), isSending = false, error = null)
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.review_order_button)).assertIsDisplayed()
        rule.onNodeWithTag(ORDER_BUTTON_TAG).assertIsEnabled()
    }
}
