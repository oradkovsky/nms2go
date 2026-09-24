package com.ror.nms2go.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SenderRepository @Inject constructor(
    private val senderDao: SenderDao
) {
    fun observeAll(): Flow<List<SenderEntity>> = senderDao.observeAll()

    suspend fun getAll(): List<SenderEntity> = senderDao.getAll()

    suspend fun insert(sender: SenderEntity) {
        senderDao.insert(sender)
    }

    suspend fun getByEmail(email: String): SenderEntity? = senderDao.getByEmail(email)

    suspend fun update(sender: SenderEntity) {
        senderDao.update(sender)
    }

    suspend fun updateByEmail(
        originalEmail: String,
        email: String,
        companyName: String,
        receiverEmail: String,
        parser: String,
        skipKeywords: String
    ): Int = senderDao.updateByEmail(
        originalEmail = originalEmail,
        email = email,
        companyName = companyName,
        receiverEmail = receiverEmail,
        parser = parser,
        skipKeywords = skipKeywords
    )

    suspend fun deleteByEmail(email: String): Int =
        senderDao.deleteByEmail(email)
}
