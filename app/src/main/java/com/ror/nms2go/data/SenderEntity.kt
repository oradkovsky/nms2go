package com.ror.nms2go.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "senders")
data class SenderEntity(
    @PrimaryKey
    @ColumnInfo(name = "email")
    val inboundEmail: String,
    @ColumnInfo(name = "company_name")
    val companyName: String,
    @ColumnInfo(name = "receiver_email")
    val outboundEmail: String = "",
    @ColumnInfo(name = "parser")
    val parser: String = "",
    @ColumnInfo(name = "skip_keywords")
    val skipKeywords: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
