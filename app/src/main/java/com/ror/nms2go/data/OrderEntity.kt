package com.ror.nms2go.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object OrderStatus {
    const val SENT = "sent"
    const val FAILED = "failed"
}

@Entity(
    tableName = "orders",
    indices = [Index(value = ["sent_at"])]
)
data class SentOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "sent_at")
    val sentAt: Long,
    @ColumnInfo(name = "company")
    val company: String,
    @ColumnInfo(name = "sender_email")
    val senderEmail: String,
    @ColumnInfo(name = "receiver_email")
    val receiverEmail: String,
    @ColumnInfo(name = "subject")
    val subject: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "error")
    val error: String?
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = SentOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["order_id"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Long,
    @ColumnInfo(name = "code")
    val code: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "price")
    val price: Double?,
    @ColumnInfo(name = "quantity")
    val quantity: Int
)

data class SentOrderWithItems(
    @Embedded
    val order: SentOrderEntity,
    @Relation(parentColumn = "id", entityColumn = "order_id")
    val items: List<OrderItemEntity>
)