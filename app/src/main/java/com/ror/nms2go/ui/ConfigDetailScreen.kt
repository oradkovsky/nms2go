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
import androidx.compose.ui.tooling.preview.Preview
import com.ror.nms2go.ExcelParser
import com.ror.nms2go.R
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.ui.theme.Nms2GoTheme

@Composable
fun ConfigDetailScreen(
    initialSender: SenderEntity?,
    onAdd: (String, String, String, String) -> Unit,
    onUpdate: (Long, String, String, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    val editing = initialSender != null

    var company by rememberSaveable { mutableStateOf(initialSender?.companyName ?: "") }
    var email by rememberSaveable { mutableStateOf(initialSender?.email ?: "") }
    var receiver by rememberSaveable { mutableStateOf(initialSender?.receiverEmail ?: "") }
    var parser by rememberSaveable { mutableStateOf(initialSender?.parser ?: "") }

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

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (editing) {
                        initialSender?.let { onUpdate(it.id, company, email, receiver, parser) }
                    } else {
                        onAdd(company, email, receiver, parser)
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
            if (editing) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        initialSender?.let { onDelete(it.id) }
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

@Preview(name = "Add - Light", showBackground = true)
@Composable
private fun ConfigDetailScreenAddPreview() {
    Nms2GoTheme {
        ConfigDetailScreen(
            initialSender = null,
            onAdd = { _, _, _, _ -> },
            onUpdate = { _, _, _, _, _ -> },
            onDelete = {},
            onBack = {}
        )
    }
}

@Preview(name = "Edit - Light", showBackground = true)
@Composable
private fun ConfigDetailScreenEditPreview() {
    Nms2GoTheme {
        ConfigDetailScreen(
            initialSender = SenderEntity(
                id = 1L,
                companyName = "Acme Corp",
                email = "billing@acme.com",
                receiverEmail = "user@example.com",
                parser = "Test Parser",
                createdAt = 1700000000000L
            ),
            onAdd = { _, _, _, _ -> },
            onUpdate = { _, _, _, _, _ -> },
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
