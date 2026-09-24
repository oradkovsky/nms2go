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

    /**
     * Updates the row identified by [originalEmail], including a possible
     * email (primary key) change. No delete is involved. A colliding new
     * email violates the primary key constraint. Returns rows affected.
     */
    @Query(
        """
        UPDATE senders SET
            email = :email,
            company_name = :companyName,
            receiver_email = :receiverEmail,
            parser = :parser,
            skip_keywords = :skipKeywords
        WHERE email = :originalEmail
        """
    )
    suspend fun updateByEmail(
        originalEmail: String,
        email: String,
        companyName: String,
        receiverEmail: String,
        parser: String,
        skipKeywords: String
    ): Int

    @Query("SELECT * FROM senders WHERE email = :email")
    suspend fun getByEmail(email: String): SenderEntity?

    @Query("DELETE FROM senders WHERE email = :email")
    suspend fun deleteByEmail(email: String): Int
}