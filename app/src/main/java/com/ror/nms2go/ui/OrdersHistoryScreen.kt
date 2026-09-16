package com.ror.nms2go.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ror.nms2go.R
import com.ror.nms2go.data.OrderItemEntity
import com.ror.nms2go.data.OrderStatus
import com.ror.nms2go.data.SentOrderEntity
import com.ror.nms2go.data.SentOrderWithItems
import com.ror.nms2go.ui.theme.ThemedPreview
import com.ror.nms2go.utils.formatOrderTimestamp

@Composable
fun OrdersHistoryScreen(
    onOrderClick: (Long) -> Unit,
    viewModel: OrdersHistoryViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    OrdersHistoryScreenContent(orders = orders, onOrderClick = onOrderClick)
}

@Composable
fun OrdersHistoryScreen(
    orders: List<SentOrderWithItems>,
    onOrderClick: (Long) -> Unit
) {
    OrdersHistoryScreenContent(orders = orders, onOrderClick = onOrderClick)
}

@Composable
private fun OrdersHistoryScreenContent(
    orders: List<SentOrderWithItems>,
    onOrderClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (orders.isEmpty()) {
            Text(
                text = stringResource(R.string.orders_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.orders_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        Text(
            text = stringResource(R.string.orders_count, orders.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(orders, key = { it.order.id }) { sent ->
                OrdersRow(sent = sent, onClick = { onOrderClick(sent.order.id) })
            }
        }
    }
}

@ThemedPreview
@Composable
private fun OrdersHistoryEmptyPreview() {
    ThemedPreview {
        OrdersHistoryScreenContent(orders = emptyList(), onOrderClick = {})
    }
}

@ThemedPreview
@Composable
private fun OrdersHistoryPopulatedPreview() {
    ThemedPreview {
        OrdersHistoryScreenContent(
            orders = listOf(
                SentOrderWithItems(
                    order = SentOrderEntity(
                        id = 1,
                        sentAt = 1_700_000_000_000L,
                        company = "Альба",
                        senderEmail = "alba@example.com",
                        receiverEmail = "pharmacy@example.com",
                        subject = "Order Альба 2024-11-14",
                        status = OrderStatus.SENT,
                        error = null
                    ),
                    items = listOf(
                        OrderItemEntity(id = 10, orderId = 1, code = "C1", name = "Парацетамол", price = 12.5, quantity = 3),
                        OrderItemEntity(id = 11, orderId = 1, code = "C2", name = "Ібупрофен", price = 95.0, quantity = 1)
                    )
                ),
                SentOrderWithItems(
                    order = SentOrderEntity(
                        id = 2,
                        sentAt = 1_700_000_100_000L,
                        company = "Вента",
                        senderEmail = "venta@example.com",
                        receiverEmail = "other@example.com",
                        subject = "Order Вента 2024-11-15",
                        status = OrderStatus.FAILED,
                        error = "no receiver configured"
                    ),
                    items = listOf(
                        OrderItemEntity(id = 20, orderId = 2, code = "C3", name = "Аспірин", price = 10.0, quantity = 2)
                    )
                )
            ),
            onOrderClick = {}
        )
    }
}

@Composable
private fun OrdersRow(
    sent: SentOrderWithItems,
    onClick: () -> Unit
) {
    val order = sent.order
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
                    text = order.company,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatOrderTimestamp(order.sentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${stringResource(R.string.orders_row_to)}: ${order.receiverEmail.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.orders_row_items,
                        sent.items.size,
                        sent.items.sumOf { it.quantity }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        if (order.status == OrderStatus.SENT) {
                            R.string.orders_status_sent
                        } else {
                            R.string.orders_status_failed
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (order.status == OrderStatus.SENT) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}