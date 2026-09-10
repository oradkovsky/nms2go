package com.ror.nms2go.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ror.nms2go.R
import com.ror.nms2go.data.OrderStatus
import com.ror.nms2go.data.SentOrderWithItems
import java.util.Locale

@Composable
fun OrderDetailScreen(
    orders: List<SentOrderWithItems>,
    orderId: Long,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val sent = orders.firstOrNull { it.order.id == orderId }
    if (sent == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.order_detail_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sent.order.company,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(
                        if (sent.order.status == OrderStatus.SENT) {
                            R.string.orders_status_sent
                        } else {
                            R.string.orders_status_failed
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (sent.order.status == OrderStatus.SENT) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatOrderTimestamp(sent.order.sentAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            DetailRow(
                label = stringResource(R.string.order_detail_sender),
                value = sent.order.senderEmail.ifBlank { sent.order.company }
            )
            DetailRow(
                label = stringResource(R.string.order_detail_receiver),
                value = sent.order.receiverEmail.ifBlank { "—" }
            )
            DetailRow(
                label = stringResource(R.string.order_detail_subject),
                value = sent.order.subject
            )
            sent.order.error?.takeIf { it.isNotBlank() }?.let { error ->
                DetailRow(
                    label = stringResource(R.string.order_detail_error),
                    value = error
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.order_detail_summary,
                    sent.items.size,
                    sent.items.sumOf { it.quantity }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn {
            items(sent.items, key = { it.id }) { item ->
                OrderItemRow(item = item)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun OrderItemRow(item: com.ror.nms2go.data.OrderItemEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "×${item.quantity}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = buildString {
                if (item.code.isNotBlank()) {
                    append(item.code)
                    append("  ·  ")
                }
                item.price?.let { append(String.format(Locale.US, "%.2f", it)) }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        HorizontalDivider()
    }
}