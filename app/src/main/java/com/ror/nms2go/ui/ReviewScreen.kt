package com.ror.nms2go.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ror.nms2go.ExcelRow
import com.ror.nms2go.R
import com.ror.nms2go.utils.debugTestTag

internal const val ORDER_BUTTON_TAG = "orderButton"

private fun Modifier.reviewScreenPadding(): Modifier =
    padding(horizontal = 16.dp, vertical = 12.dp)

@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    onOrderRequested: () -> Unit,
    onConfirmOrder: () -> Unit,
    onDismissConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val showConfirm = (uiState as? ReviewUiState.Content)?.showConfirm == true
    BackHandler(enabled = showConfirm) { onDismissConfirm() }
    BackHandler(enabled = !showConfirm) { onBack() }

    when (uiState) {
        is ReviewUiState.Empty -> {
            ReviewEmpty(onBack = onBack)
        }

        is ReviewUiState.Content -> {
            ReviewContent(
                uiState = uiState,
                onQuantityChange = onQuantityChange,
                onOrderRequested = onOrderRequested
            )
        }

        is ReviewUiState.Error -> {
            ReviewError(message = uiState.message, onBack = onBack)
        }
    }

    if (showConfirm) {
        ReviewConfirmDialog(
            onConfirm = onConfirmOrder,
            onDismiss = onDismissConfirm
        )
    }
}

@Composable
private fun ReviewEmpty(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .reviewScreenPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.review_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun ReviewError(message: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .reviewScreenPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun ReviewContent(
    uiState: ReviewUiState.Content,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    onOrderRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .reviewScreenPadding()
    ) {
        Text(
            text = stringResource(R.string.review_summary, uiState.chosen.size, uiState.totalUnits),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(uiState.chosen, key = { it.index }) { indexedRow ->
                val index = indexedRow.index
                ParsedRowView(
                    row = indexedRow.value,
                    quantity = uiState.quantities[index] ?: 0,
                    onQuantityChange = { newQuantity ->
                        onQuantityChange(index, newQuantity)
                    },
                    enabled = !uiState.isSending
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onOrderRequested,
            modifier = Modifier
                .fillMaxWidth()
                .debugTestTag(ORDER_BUTTON_TAG),
            // No sending while in progress, and nothing to send when all quantities are zero.
            enabled = !uiState.isSending && uiState.totalUnits > 0
        ) {
            if (uiState.isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = stringResource(
                    if (uiState.isSending) R.string.review_sending else R.string.review_order_button
                )
            )
        }
    }
}

@Composable
private fun ReviewConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.review_order_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.review_order_dialog_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.review_order_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.review_order_dialog_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ReviewContentPreview() {
    val row = ExcelRow(
        counteragent = "Test Co",
        article = "Item A",
        vendor = "Vendor",
        price = 100.0,
        vat = "Так",
        code = "Item A | Test Co",
        receiver = "receiver@example.com",
        company = "Test Supplier"
    )
    MaterialTheme {
        ReviewContent(
            uiState = ReviewUiState.Content(
                chosen = listOf(IndexedValue(0, row)),
                quantities = mapOf(0 to 2),
                totalUnits = 2,
                isSending = false,
                showConfirm = false
            ),
            onQuantityChange = { _, _ -> },
            onOrderRequested = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewEmptyPreview() {
    MaterialTheme {
        ReviewEmpty(onBack = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewErrorPreview() {
    MaterialTheme {
        ReviewError(message = "boom", onBack = {})
    }
}
