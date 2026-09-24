package com.ror.nms2go.domain

import android.database.sqlite.SQLiteConstraintException
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveSenderUseCaseTest {

    private val dao = InMemorySenderDao()
    private val repository = SenderRepository(dao)
    private val useCase = SaveSenderUseCase(repository)

    @Test
    fun insertNewSender_returnsSavedAndStoresTrimmed() = runTest {
        val result = useCase(
            originalEmail = null,
            companyName = "  Acme Corp ",
            email = " billing@acme.com ",
            receiverEmail = " orders@acme.com ",
            parser = "PDF_PARSER_V1",
            skipKeywords = " gift "
        )

        assertEquals(SaveSenderResult.Saved, result)
        val stored = dao.getByEmail("billing@acme.com")
        assertEquals("Acme Corp", stored?.companyName)
        assertEquals("billing@acme.com", stored?.email)
        assertEquals("orders@acme.com", stored?.receiverEmail)
        assertEquals("PDF_PARSER_V1", stored?.parser)
        assertEquals("gift", stored?.skipKeywords)
    }

    @Test
    fun blankCompany_returnsInvalidInputAndStoresNothing() = runTest {
        val result = useCase(
            originalEmail = null,
            companyName = "   ",
            email = "billing@acme.com",
            receiverEmail = "",
            parser = ""
        )

        assertEquals(SaveSenderResult.InvalidInput, result)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun blankEmail_returnsInvalidInputAndStoresNothing() = runTest {
        val result = useCase(
            originalEmail = null,
            companyName = "Acme Corp",
            email = "  ",
            receiverEmail = "",
            parser = ""
        )

        assertEquals(SaveSenderResult.InvalidInput, result)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun sameKeyEdit_updatesFieldsAndPreservesCreatedAt() = runTest {
        dao.seed(sender(email = "billing@acme.com", createdAt = 123L))

        val result = useCase(
            originalEmail = "billing@acme.com",
            companyName = "Acme Updated",
            email = "billing@acme.com",
            receiverEmail = "new@acme.com",
            parser = "PDF_PARSER_V1"
        )

        assertEquals(SaveSenderResult.Saved, result)
        val stored = dao.getByEmail("billing@acme.com")
        assertEquals("Acme Updated", stored?.companyName)
        assertEquals("new@acme.com", stored?.receiverEmail)
        assertEquals(123L, stored?.createdAt)
    }

    @Test
    fun rename_movesRowAndPreservesCreatedAt() = runTest {
        dao.seed(sender(email = "old@acme.com", createdAt = 456L))

        val result = useCase(
            originalEmail = "old@acme.com",
            companyName = "Acme Corp",
            email = "new@acme.com",
            receiverEmail = "",
            parser = ""
        )

        assertEquals(SaveSenderResult.Saved, result)
        assertNull(dao.getByEmail("old@acme.com"))
        val stored = dao.getByEmail("new@acme.com")
        assertEquals("Acme Corp", stored?.companyName)
        assertEquals(456L, stored?.createdAt)
    }

    @Test
    fun insertDuplicate_returnsDuplicateEmailAndKeepsOriginal() = runTest {
        dao.seed(sender(email = "billing@acme.com", company = "Acme Corp"))

        val result = useCase(
            originalEmail = null,
            companyName = "Intruder",
            email = "billing@acme.com",
            receiverEmail = "",
            parser = ""
        )

        assertEquals(SaveSenderResult.DuplicateEmail, result)
        assertEquals("Acme Corp", dao.getByEmail("billing@acme.com")?.companyName)
    }

    @Test
    fun renameOntoExisting_returnsDuplicateEmailAndKeepsBothRows() = runTest {
        dao.seed(
            sender(email = "a@acme.com", company = "A"),
            sender(email = "b@acme.com", company = "B")
        )

        val result = useCase(
            originalEmail = "a@acme.com",
            companyName = "A renamed",
            email = "b@acme.com",
            receiverEmail = "",
            parser = ""
        )

        assertEquals(SaveSenderResult.DuplicateEmail, result)
        assertEquals("A", dao.getByEmail("a@acme.com")?.companyName)
        assertEquals("B", dao.getByEmail("b@acme.com")?.companyName)
    }

    @Test
    fun constraintWithoutConflictingRow_returnsStorageError() = runTest {
        dao.failInserts = true

        val result = useCase(
            originalEmail = null,
            companyName = "Acme Corp",
            email = "billing@acme.com",
            receiverEmail = "",
            parser = ""
        )

        assertTrue(result is SaveSenderResult.StorageError)
        assertSame(dao.lastFailure, (result as SaveSenderResult.StorageError).cause)
    }

    @Test
    fun sameKeyUpdateConstraint_returnsStorageErrorNotDuplicate() = runTest {
        dao.seed(sender(email = "billing@acme.com"))
        dao.failUpdates = true

        val result = useCase(
            originalEmail = "billing@acme.com",
            companyName = "Acme Corp",
            email = "billing@acme.com",
            receiverEmail = "",
            parser = ""
        )

        // The only row under that email is the edited row itself,
        // so this cannot be a duplicate – it must surface as storage error.
        assertTrue(result is SaveSenderResult.StorageError)
    }

    @Test
    fun deleteByEmail_returnsRemovedRowCount() = runTest {
        dao.seed(sender(email = "billing@acme.com"))

        assertEquals(1, repository.deleteByEmail("billing@acme.com"))
        assertEquals(0, repository.deleteByEmail("billing@acme.com"))
        assertEquals(0, repository.deleteByEmail("missing@acme.com"))
    }

    private fun sender(
        email: String,
        company: String = "Acme Corp",
        createdAt: Long = 1_000L
    ) = SenderEntity(
        companyName = company,
        email = email,
        createdAt = createdAt
    )

    /**
     * In-memory [SenderDao] mirroring Room semantics: inserts and renames
     * onto a taken email throw [SQLiteConstraintException], updates of
     * missing rows affect 0 rows, deletes report the removed row count.
     */
    private class InMemorySenderDao : SenderDao {
        private val rows = mutableListOf<SenderEntity>()
        private val flow = MutableStateFlow<List<SenderEntity>>(emptyList())
        var failInserts = false
        var failUpdates = false
        var lastFailure: SQLiteConstraintException? = null

        fun seed(vararg senders: SenderEntity) {
            rows.addAll(senders)
            flow.value = rows.toList()
        }

        private fun fail(): Nothing {
            val error = SQLiteConstraintException("UNIQUE constraint failed: senders.email")
            lastFailure = error
            throw error
        }

        override fun observeAll(): Flow<List<SenderEntity>> = flow

        override suspend fun getAll(): List<SenderEntity> = rows.toList()

        override suspend fun insert(sender: SenderEntity): Long {
            if (failInserts || rows.any { it.email == sender.email }) fail()
            rows += sender
            flow.value = rows.toList()
            return rows.size.toLong()
        }

        override suspend fun update(sender: SenderEntity) {
            val index = rows.indexOfFirst { it.email == sender.email }
            if (index >= 0) {
                rows[index] = sender
                flow.value = rows.toList()
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
            val index = rows.indexOfFirst { it.email == originalEmail }
            if (index < 0) return 0
            if (failUpdates || (email != originalEmail && rows.any { it.email == email })) fail()
            rows[index] = rows[index].copy(
                email = email,
                companyName = companyName,
                receiverEmail = receiverEmail,
                parser = parser,
                skipKeywords = skipKeywords
            )
            flow.value = rows.toList()
            return 1
        }

        override suspend fun getByEmail(email: String): SenderEntity? =
            rows.firstOrNull { it.email == email }

        override suspend fun deleteByEmail(email: String): Int {
            val before = rows.size
            rows.removeAll { it.email == email }
            flow.value = rows.toList()
            return before - rows.size
        }
    }
}
