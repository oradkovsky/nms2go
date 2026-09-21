package com.ror.nms2go.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ror.nms2go.ExcelParser
import com.ror.nms2go.R
import com.ror.nms2go.ui.theme.ThemedPreview

@Composable
fun ConfigDetailScreen(
    uiState: ConfigDetailUiState,
    onAdd: (String, String, String, String, String) -> Unit,
    onUpdate: (Long, String, String, String, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    when (uiState) {
        ConfigDetailUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ConfigDetailUiState.NotFound -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.config_not_found))
            }
        }

        is ConfigDetailUiState.Content -> {
            DetailForm(
                item = uiState.item,
                isEditing = uiState.isEditing,
                onAdd = onAdd,
                onUpdate = onUpdate,
                onDelete = onDelete,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun DetailForm(
    item: ConfigDetailItem,
    isEditing: Boolean,
    onAdd: (String, String, String, String, String) -> Unit,
    onUpdate: (Long, String, String, String, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    // Keyed on the item id so fields re-initialize if a different item arrives.
    var company by rememberSaveable(item.id) { mutableStateOf(item.company) }
    var email by rememberSaveable(item.id) { mutableStateOf(item.email) }
    var receiver by rememberSaveable(item.id) { mutableStateOf(item.receiver) }
    var parser by rememberSaveable(item.id) { mutableStateOf(item.parser) }
    var skipKeywords by rememberSaveable(item.id) { mutableStateOf(item.skipKeywords) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp)
    ) {
        OutlinedTextField(
            value = company,
            onValueChange = { company = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.config_company_label)) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.config_sender_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = receiver,
            onValueChange = { receiver = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.config_receiver_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(8.dp))
        ParserDropdown(parser = parser, onParserChange = { parser = it })

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = skipKeywords,
            onValueChange = { skipKeywords = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.config_skip_keywords_label)) },
            supportingText = { Text(stringResource(R.string.config_skip_keywords_hint)) },
            singleLine = false,
            minLines = 1
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (isEditing) {
                        onUpdate(item.id, company, email, receiver, parser, skipKeywords)
                    } else {
                        onAdd(company, email, receiver, parser, skipKeywords)
                    }
                    onBack()
                },
                enabled = company.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.config_save_button),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isEditing) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        onDelete(item.id)
                        onBack()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.config_remove_button),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@ThemedPreview
@Composable
private fun ConfigDetailScreenLoadingPreview() {
    ThemedPreview {
        ConfigDetailScreen(
            uiState = ConfigDetailUiState.Loading,
            onAdd = { _, _, _, _, _ -> },
            onUpdate = { _, _, _, _, _, _ -> },
            onDelete = {},
            onBack = {}
        )
    }
}

@ThemedPreview
@Composable
private fun ConfigDetailScreenAddPreview() {
    ThemedPreview {
        ConfigDetailScreen(
            uiState = ConfigDetailUiState.Content(ConfigDetailItem(), isEditing = false),
            onAdd = { _, _, _, _, _ -> },
            onUpdate = { _, _, _, _, _, _ -> },
            onDelete = {},
            onBack = {}
        )
    }
}

@ThemedPreview
@Composable
private fun ConfigDetailScreenEditPreview() {
    ThemedPreview {
        ConfigDetailScreen(
            uiState = ConfigDetailUiState.Content(
                ConfigDetailItem(
                    id = 1L,
                    company = "Acme Corp",
                    email = "billing@acme.com",
                    receiver = "user@example.com",
                    parser = ""
                ),
                isEditing = true
            ),
            onAdd = { _, _, _, _, _ -> },
            onUpdate = { _, _, _, _, _, _ -> },
            onDelete = {},
            onBack = {}
        )
    }
}

@ThemedPreview
@Composable
private fun ConfigDetailScreenNotFoundPreview() {
    ThemedPreview {
        ConfigDetailScreen(
            uiState = ConfigDetailUiState.NotFound,
            onAdd = { _, _, _, _, _ -> },
            onUpdate = { _, _, _, _, _, _ -> },
            onDelete = {},
            onBack = {}
        )
    }
}

@Composable
private fun ParserDropdown(
    parser: String,
    onParserChange: (String) -> Unit
) {
    val autoLabel = stringResource(R.string.config_parser_auto)
    val options = remember { listOf("" to autoLabel) + ExcelParser.parserOptions() }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == parser }?.second ?: parser,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.config_parser_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        onParserChange(option.first)
                        expanded = false
                    }
                )
            }
        }
    }
}
