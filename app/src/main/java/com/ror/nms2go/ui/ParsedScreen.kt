package com.ror.nms2go.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ror.nms2go.R
import java.util.Locale

@Composable
fun ParsedScreen(
    uiState: ParsedUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onPageChange: (Int) -> Unit,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    when (uiState) {
        is ParsedUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.parsed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is ParsedUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                uiState.progress?.let { (loaded, total) ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (total > 0) stringResource(
                            R.string.parsed_loading_progress,
                            loaded,
                            total
                        ) else stringResource(R.string.parsed_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        is ParsedUiState.Content -> ParsedContent(
            uiState = uiState,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
            onPageChange = onPageChange,
            onQuantityChange = onQuantityChange
        )
    }
}

@Composable
private fun ParsedContent(
    uiState: ParsedUiState.Content,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onPageChange: (Int) -> Unit,
    onQuantityChange: (index: Int, quantity: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = uiState.summaryText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (uiState.isLoadingMore) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            uiState.orderLoadingProgress?.let { (loaded, total) ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (total > 0) stringResource(
                        R.string.parsed_loading_progress,
                        loaded,
                        total
                    ) else stringResource(R.string.parsed_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.parsed_filter_hint)) },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.parsed_filter_clear)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .debugTestTag("parsedFilter")
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.parsed_qty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (uiState.filteredSize == 0) {
            Text(
                text = stringResource(R.string.parsed_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.parsed_page_info, uiState.filteredSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(uiState.pageItems) { _, indexedRow ->
                    val index = indexedRow.index
                    ParsedRowView(
                        row = indexedRow.value,
                        quantity = uiState.quantities[index] ?: 0,
                        onQuantityChange = { newQuantity ->
                            onQuantityChange(index, newQuantity)
                        }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compact = maxWidth < 360.dp
                val buttonPadding = if (compact) 8.dp else 12.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onPageChange(uiState.page - 1) },
                        enabled = uiState.page > 0,
                        contentPadding = PaddingValues(horizontal = buttonPadding)
                    ) {
                        Text(
                            text = stringResource(R.string.parsed_prev),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.parsed_page_of,
                            uiState.page + 1,
                            uiState.totalPages
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    TextButton(
                        onClick = { onPageChange(uiState.page + 1) },
                        enabled = uiState.page < uiState.totalPages - 1,
                        contentPadding = PaddingValues(horizontal = buttonPadding)
                    ) {
                        Text(
                            text = stringResource(R.string.parsed_next),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ParsedRowView(
    row: com.ror.nms2go.ExcelRow,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (row.counteragent.isNotBlank()) {
                Text(
                    text = row.counteragent,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = row.article,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (row.vendor.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = row.vendor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.price?.let { String.format(Locale.US, "%.2f", it) }.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                QuantityStepper(
                    quantity = quantity,
                    onQuantityChange = onQuantityChange
                )
            }
        }
    }
}

@Composable
internal fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf(TextFieldValue("")) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    val errorInvalid = stringResource(R.string.quantity_dialog_error_invalid)
    val errorRange = stringResource(R.string.quantity_dialog_error_range)

    Row(
        modifier = Modifier
            .width(128.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(
            enabled = quantity > 0,
            onClick = { onQuantityChange(quantity - 1) }
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        VerticalDivider(
            modifier = Modifier.height(26.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clickable {
                    val text = quantity.toString()
                    dialogText = TextFieldValue(
                        text = text,
                        selection = TextRange(0, text.length)
                    )
                    dialogError = null
                    showDialog = true
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        VerticalDivider(
            modifier = Modifier.height(26.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        StepperButton(
            enabled = quantity < MAX_QUANTITY,
            onClick = { onQuantityChange(quantity + 1) }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.parsed_qty_add)
            )
        }
    }

    if (showDialog) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.quantity_dialog_title)) },
            text = {
                // Request focus + show keyboard once dialog content is composed.
                // Scoped to dialog lifecycle (LaunchedEffect(Unit) inside if(showDialog)),
                // so no delay hack or outer LaunchedEffect(showDialog) needed.
                LaunchedEffect(Unit) {
                    try {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (_: IllegalStateException) {
                        // FocusRequester not yet attached - safe to ignore
                    }
                }
                OutlinedTextField(
                    value = dialogText,
                    onValueChange = { newValue ->
                        // Allow only digits, empty is allowed for editing
                        val filtered = newValue.text.filter { it.isDigit() }
                        // Limit to MAX_QUANTITY digits to avoid overflow
                        if (filtered.length <= MAX_QUANTITY.toString().length) {
                            val parsed = filtered.toIntOrNull()
                            dialogError = when {
                                filtered.isEmpty() -> null
                                parsed == null -> errorInvalid
                                parsed > MAX_QUANTITY -> errorRange
                                else -> null
                            }
                            dialogText = newValue.copy(
                                text = filtered,
                                selection = TextRange(filtered.length)
                            )
                        }
                    },
                    singleLine = true,
                    isError = dialogError != null,
                    supportingText = {
                        if (dialogError != null) {
                            Text(dialogError!!)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = dialogText.text.toIntOrNull()
                        if (parsed != null && parsed in 0..MAX_QUANTITY) {
                            onQuantityChange(parsed)
                            showDialog = false
                        } else if (dialogText.text.isEmpty()) {
                            onQuantityChange(0)
                            showDialog = false
                        }
                    },
                    enabled = dialogError == null && dialogText.text.toIntOrNull() != null && dialogText.text.toIntOrNull() in 0..MAX_QUANTITY
                ) {
                    Text(stringResource(R.string.quantity_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.quantity_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun StepperButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(40.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
