package com.ror.nms2go.data

import javax.inject.Inject

data class OrderHistoryRecord(
    val company: String,
    val senderEmail: String,
    val receiverEmail: String,
    val subject: String,
    val status: String,
    val error: String?,
    val items: List<OrderHistoryItem>
)

data class OrderHistoryItem(
    val code: String,
    val name: String,
    val price: Double?,
    val quantity: Int
)

class OrderHistoryRepository @Inject constructor(
    private val orderDao: OrderDao
) {
    suspend fun record(record: OrderHistoryRecord) {
        val orderId = orderDao.insertOrder(
            SentOrderEntity(
                sentAt = System.currentTimeMillis(),
                company = record.company,
                senderEmail = record.senderEmail,
                receiverEmail = record.receiverEmail,
                subject = record.subject,
                status = record.status,
                error = record.error
            )
        )
        orderDao.insertOrderItems(
            record.items.map { item ->
                OrderItemEntity(
                    orderId = orderId,
                    code = item.code,
                    name = item.name,
                    price = item.price,
                    quantity = item.quantity
                )
            }
        )
    }
}
