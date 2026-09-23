package com.ror.nms2go

import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide sender state for Hilt UI tests. Synchronous: tests drive it by
 * setting [senders] directly, with no Room schedulers or runBlocking involved.
 * Must be [reset] in every test's @Before - the Hilt singleton outlives a
 * single test.
 */
object FakeSenders {
    val senders = MutableStateFlow<List<SenderEntity>>(emptyList())

    fun reset() {
        senders.value = emptyList()
    }
}

class FakeSenderDao : SenderDao {
    override fun observeAll(): Flow<List<SenderEntity>> = FakeSenders.senders

    override suspend fun getAll(): List<SenderEntity> = FakeSenders.senders.value

    override suspend fun insert(sender: SenderEntity): Long {
        FakeSenders.senders.update { it + sender }
        return sender.id
    }

    override suspend fun update(sender: SenderEntity) {
        FakeSenders.senders.update { list ->
            list.map { if (it.id == sender.id) sender else it }
        }
    }

    override suspend fun getById(id: Long): SenderEntity? =
        FakeSenders.senders.value.firstOrNull { it.id == id }

    override suspend fun deleteById(id: Long) {
        FakeSenders.senders.update { list -> list.filterNot { it.id == id } }
    }
}
