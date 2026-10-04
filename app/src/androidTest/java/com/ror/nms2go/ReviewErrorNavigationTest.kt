package com.ror.nms2go

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.ror.nms2go.data.GmailAuthManager
import com.ror.nms2go.ui.Nms2GoApp
import com.ror.nms2go.ui.ORDER_BUTTON_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Navigation-level coverage for [com.ror.nms2go.ui.ReviewScreen]'s error state,
 * following the real user flow: Overview auto-navigates to Parsed once items
 * are picked, the cart icon opens Review, confirming starts the send, and the
 * failed send outcome forces `ReviewUiState.Error`. Both the on-screen back
 * button and the system back must return to Parsed.
 */
@HiltAndroidTest
class ReviewErrorNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun init() {
        hiltRule.inject()
        FakeSenders.reset()
        fakeGmail.reset()
        fakeGmail.failWith = "boom"
    }

    @Inject
    lateinit var fakeGmail: FakeGmailSender

    @Inject
    lateinit var authManager: GmailAuthManager

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun setUp() {
        rule.setContent {
            Nms2GoApp(
                loading = false,
                statusText = "",
                overviewResults = emptyList(),
                onLoad = {},
                onParseItem = { _ -> },
                onOrder = {},
                parsedExcel = ParsedExcel(
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
                ),
                onDismissParsed = {},
                orderQuantities = mapOf(0 to 2),
                onQuantityChange = { _, _ -> }
            )
        }
        rule.waitForIdle()
        TestVisuals.afterSetContent()
    }

    private fun navigateToReviewError() {
        // Parsed is the item-picking screen preceding Review.
        rule.onNodeWithText(appContext.getString(R.string.parsed_title)).assertIsDisplayed()

        rule.onNodeWithContentDescription(appContext.getString(R.string.review_title)).performClick()
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithTag(ORDER_BUTTON_TAG).performClick()
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onNodeWithText(appContext.getString(R.string.review_order_dialog_confirm)).performClick()
        rule.waitForIdle()
        TestVisuals.afterAction()

        authManager.onAuthorizationResult("fake-token")
        rule.waitForIdle()
        TestVisuals.afterAction()

        rule.onNodeWithText("boom", substring = true).assertIsDisplayed()
    }

    private fun assertReturnedToParsed() {
        rule.onNodeWithText("boom", substring = true).assertDoesNotExist()
        rule.onNodeWithText(appContext.getString(R.string.parsed_title)).assertIsDisplayed()
    }

    @Test
    fun reviewError_backButton_returnsToParsed() {
        setUp()
        navigateToReviewError()

        rule.onNodeWithText(appContext.getString(R.string.back)).performClick()
        rule.waitForIdle()
        TestVisuals.afterAction()

        assertReturnedToParsed()
    }

    @Test
    fun reviewError_systemBack_returnsToParsed() {
        setUp()
        navigateToReviewError()

        Espresso.pressBack()
        rule.waitForIdle()
        TestVisuals.afterAction()

        assertReturnedToParsed()
    }
}
