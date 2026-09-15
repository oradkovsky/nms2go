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
}
