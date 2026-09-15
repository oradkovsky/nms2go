package com.ror.nms2go.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderRepository @Inject constructor(
    private val senderDao: SenderDao
) {
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
