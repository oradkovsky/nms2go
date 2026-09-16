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

    suspend fun getById(id: Long): SenderEntity? = senderDao.getById(id)

    suspend fun update(sender: SenderEntity) {
        senderDao.update(sender)
    }

    suspend fun deleteById(id: Long) {
        senderDao.deleteById(id)
    }
}
