package com.ror.nms2go

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ror.nms2go.ui.ConfigDetailItem
import com.ror.nms2go.ui.ConfigDetailScreen
import com.ror.nms2go.ui.ConfigDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Regression tests: the detail screen used to stay blank when the sender arrived
 * after first composition (async DB load), because field states were initialized
 * from null and never re-initialized. The screen is now driven by
 * [ConfigDetailUiState] emitted from the ViewModel.
 */
class ConfigDetailLateLoadTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun fieldsPopulate_whenContentArrivesAfterLoading() {
        val uiState = MutableStateFlow<ConfigDetailUiState>(ConfigDetailUiState.Loading)
        rule.setContent {
            val state by uiState.collectAsState()
            ConfigDetailScreen(
                uiState = state,
                onAdd = { _, _, _, _, _ -> },
                onUpdate = { _, _, _, _, _, _ -> },
                onDelete = {},
                onBack = {}
            )
        }

        // Simulate the async DB load completing after first composition.
        uiState.value = ConfigDetailUiState.Content(
            ConfigDetailItem(
                company = "Acme Corp",
                inboundEmail = "billing@acme.com",
                outboundEmail = "orders@acme.com",
                parser = "",
                skipKeywords = "відмови"
            ),
            isEditing = true
        )
        rule.waitForIdle()

        rule.onNodeWithText("Acme Corp").assertIsDisplayed()
        rule.onNodeWithText("billing@acme.com").assertIsDisplayed()
        rule.onNodeWithText("orders@acme.com").assertIsDisplayed()
        rule.onNodeWithText("відмови").assertIsDisplayed()
    }

    @Test
    fun formHidden_whileLoading() {
        rule.setContent {
            ConfigDetailScreen(
                uiState = ConfigDetailUiState.Loading,
                onAdd = { _, _, _, _, _ -> },
                onUpdate = { _, _, _, _, _, _ -> },
                onDelete = {},
                onBack = {}
            )
        }

        // No empty form fields are shown while loading.
        rule.onNodeWithText("Acme Corp").assertDoesNotExist()
    }
}
