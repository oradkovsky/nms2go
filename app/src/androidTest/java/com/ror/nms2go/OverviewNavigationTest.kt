package com.ror.nms2go

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
                loading = false,
                statusText = "",
                overviewResults = emptyList(),
                onLoad = {},
                onParseItem = { _ -> },
                onOrder = {},
                parsedExcel = null,
                onDismissParsed = {},
                orderQuantities = emptyMap(),
                onQuantityChange = { _, _ -> }
            )
        }
        TestVisuals.afterSetContent()

        rule.onNodeWithText(appContext.getString(R.string.overview_empty_prefix), substring = true)
            .assertIsDisplayed()
        rule.onNodeWithTag("emptyStateLink").assertIsDisplayed()
        // Link-only behavior: tapping the non-linked prefix must NOT navigate …
        rule.onNodeWithTag("emptyStateLink").performTouchInput {
            click(Offset(10f, 10f))
        }
        rule.waitForIdle()
        TestVisuals.afterAction()
        rule.onAllNodesWithText(appContext.getString(R.string.config_add_button))
            .assertCountEquals(0)
        rule.onNodeWithText(appContext.getString(R.string.overview_empty_prefix), substring = true)
            .assertIsDisplayed()
        // … while tapping the link itself navigates to Configuration.
        // In the unmerged tree the link is exposed as its own node carrying
        // exactly the link text, so target it directly for a link-only tap.
        val linkText = appContext.getString(R.string.menu_configuration)
        rule.onNodeWithText(linkText, substring = false, useUnmergedTree = true)
            .performClick()
        TestVisuals.afterAction()

        rule.onNodeWithText(appContext.getString(R.string.config_add_button))
            .assertIsDisplayed()
    }
}
