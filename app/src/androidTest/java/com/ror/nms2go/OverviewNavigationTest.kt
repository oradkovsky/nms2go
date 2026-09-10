package com.ror.nms2go

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.ror.nms2go.ui.Nms2GoApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverviewNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun coldStartEmptyState_configLinkNavigatesToConfiguration() {
        rule.setContent {
            Nms2GoApp(
                senders = emptyList(),
                onAddSender = { _, _, _, _ -> },
                onUpdateSender = { _, _, _, _, _ -> },
                onRemoveSender = {},
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
                orders = emptyList(),
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
