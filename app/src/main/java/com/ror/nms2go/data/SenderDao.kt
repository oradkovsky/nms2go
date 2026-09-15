package com.ror.nms2go.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderDao {
    @Query("SELECT * FROM senders ORDER BY created_at ASC")
    fun observeAll(): Flow<List<SenderEntity>>

    @Query("SELECT * FROM senders ORDER BY created_at ASC")
    suspend fun getAll(): List<SenderEntity>

    @Insert
    suspend fun insert(sender: SenderEntity): Long

    @Update
    suspend fun update(sender: SenderEntity)

    @Query("SELECT * FROM senders WHERE id = :id")
    suspend fun getById(id: Long): SenderEntity?

    @Query("DELETE FROM senders WHERE id = :id")
    suspend fun deleteById(id: Long)
}