package com.ror.nms2go.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ror.nms2go.BuildConfig
import com.ror.nms2go.R
import com.ror.nms2go.data.QrCodec
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.ui.theme.ThemedPreview

@Composable
fun ConfigScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onScanQr: () -> Unit,
    viewModel: ConfigViewModel = hiltViewModel()
) {
    val senders by viewModel.senders.collectAsStateWithLifecycle()
    ConfigScreenContent(senders = senders, onAdd = onAdd, onEdit = onEdit, onScanQr = onScanQr)
}

@Composable
private fun ConfigScreenContent(
    senders: List<SenderEntity>,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onScanQr: () -> Unit
) {
    var showQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { showQrDialog = true },
                enabled = senders.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.config_qr_show))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onScanQr,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.config_qr_scan))
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )
            Spacer(Modifier.width(4.dp))
            Text(text = stringResource(R.string.config_add_button))
        }

        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(visible = senders.isEmpty()) {
            Text(
                text = stringResource(R.string.config_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(senders, key = { it.id }) { sender ->
                SenderRow(
                    sender = sender,
                    onClick = { onEdit(sender.id) }
                )
            }
        }

        Text(
            text = stringResource(R.string.config_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showQrDialog) {
        QrCodeDialog(senders = senders, onDismiss = { showQrDialog = false })
    }
}

@Composable
private fun QrCodeDialog(
    senders: List<SenderEntity>,
    onDismiss: () -> Unit
) {
    val qrContent = remember(senders) { QrCodec.encode(senders) }
    val qrBitmap = remember(qrContent) { generateQrCode(qrContent, 512) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.config_qr_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.config_qr_dialog_image),
                        modifier = Modifier.size(280.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.config_qr_generation_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.config_qr_dialog_hint, senders.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.parsed_close))
            }
        }
    )
}

@Composable
private fun SenderRow(
    sender: SenderEntity,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sender.companyName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.config_row_sender)}: ${sender.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stringResource(R.string.config_row_recipient)}: ${sender.receiverEmail.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@ThemedPreview
@Composable
private fun ConfigScreenPopulatedPreview() {
    ThemedPreview {
        ConfigScreenContent(
            senders = listOf(
                SenderEntity(
                    id = 1L,
                    companyName = "Acme Corp",
                    email = "billing@acme.com",
                    receiverEmail = "user@example.com",
                    parser = "PDF_PARSER_V1",
                    createdAt = 1700000000000L
                )
            ),
            onAdd = {},
            onEdit = {},
            onScanQr = {}
        )
    }
}

@ThemedPreview
@Composable
private fun ConfigScreenEmptyPreview() {
    ThemedPreview {
        ConfigScreenContent(
            senders = emptyList(),
            onAdd = {},
            onEdit = {},
            onScanQr = {}
        )
    }
}