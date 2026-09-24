package com.ror.nms2go

import android.database.sqlite.SQLiteConstraintException
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
        return 1L
    }

    override suspend fun update(sender: SenderEntity) {
        FakeSenders.senders.update { list ->
            list.map { if (it.email == sender.email) sender else it }
        }
    }

    override suspend fun updateByEmail(
        originalEmail: String,
        email: String,
        companyName: String,
        receiverEmail: String,
        parser: String,
        skipKeywords: String
    ): Int {
        val current = FakeSenders.senders.value
        val existing = current.firstOrNull { it.email == originalEmail } ?: return 0
        if (email != originalEmail && current.any { it.email == email }) {
            throw SQLiteConstraintException("Duplicate sender email: $email")
        }
        FakeSenders.senders.update { list ->
            list.map {
                if (it.email == originalEmail) {
                    existing.copy(
                        email = email,
                        companyName = companyName,
                        receiverEmail = receiverEmail,
                        parser = parser,
                        skipKeywords = skipKeywords
                    )
                } else {
                    it
                }
            }
        }
        return 1
    }

    override suspend fun getByEmail(email: String): SenderEntity? =
        FakeSenders.senders.value.firstOrNull { it.email == email }

    override suspend fun deleteByEmail(email: String): Int {
        val current = FakeSenders.senders.value
        val remaining = current.filterNot { it.email == email }
        FakeSenders.senders.value = remaining
        return current.size - remaining.size
    }
}
