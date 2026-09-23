package com.ror.nms2go

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.ror.nms2go.ui.Nms2GoApp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OverviewNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun init() {
        hiltRule.inject()
        FakeSenders.reset()
    }

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun coldStartEmptyState_configLinkNavigatesToConfiguration() {
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
                orderSentStamp = 0,
                sendingOrders = false,
                orderSendError = null
            )
        }
        TestVisuals.afterSetContent()

        rule.onNodeWithText(appContext.getString(R.string.overview_empty_prefix), substring = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("emptyStateLink").assertIsDisplayed()
        rule.onNodeWithTag("emptyStateLink").performClick()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.config_add_button))
            .assertIsDisplayed()
    }
}
