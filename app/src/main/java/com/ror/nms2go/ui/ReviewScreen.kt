package com.ror.nms2go.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ror.nms2go.R
import com.ror.nms2go.utils.debugTestTag

internal const val ORDER_BUTTON_TAG = "orderButton"

@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    onOrderRequested: () -> Unit,
    onConfirmOrder: () -> Unit,
    onDismissConfirm: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        when (uiState) {
            is ReviewUiState.Empty -> {
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
            is ReviewUiState.Content -> {
                Text(
                    text = stringResource(R.string.review_summary, uiState.chosen.size, uiState.totalUnits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
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
                    enabled = !uiState.isSending
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
            is ReviewUiState.Error -> {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.back))
                }
            }
        }
    }

    val showConfirm = (uiState as? ReviewUiState.Content)?.showConfirm == true
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = {
                Text(text = stringResource(R.string.review_order_dialog_title))
            },
            text = {
                Text(text = stringResource(R.string.review_order_dialog_message))
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmOrder
                ) {
                    Text(text = stringResource(R.string.review_order_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirm) {
                    Text(text = stringResource(R.string.review_order_dialog_cancel))
                }
            }
        )
    }
}
