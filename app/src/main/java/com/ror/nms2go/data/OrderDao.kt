package com.ror.nms2go.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: SentOrderEntity): Long

    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    @Query("SELECT * FROM orders ORDER BY sent_at DESC")
    fun observeAllOrders(): Flow<List<SentOrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeOrder(id: Long): Flow<SentOrderWithItems?>
}