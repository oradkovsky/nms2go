package com.ror.nms2go.domain

import com.ror.nms2go.data.QrSender
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the normalize/match/persist logic via [ImportSendersFromQrUseCase.importDecoded].
 * The raw-string entry point is deliberately untested here: `QrCodec` uses
 * `org.json`, which throws "not mocked" in local JVM unit tests (same reason
 * there are no `QrCodec` unit tests). Blank input short-circuits before
 * touching `org.json`, so that path is covered.
 */
class ImportSendersFromQrUseCaseTest {

    private val dao = InMemorySenderDao()
    private val repository = SenderRepository(dao)
    private val useCase = ImportSendersFromQrUseCase(repository)

    @Test
    fun blankRaw_returnsInvalidQrAndStoresNothing() = runTest {
        assertEquals(ImportSendersFromQrResult.InvalidQr, useCase("   "))
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun validItems_insertsTrimmedSendersAndReturnsSuccess() = runTest {
        val items = listOf(
            QrSender("  Acme ", " billing@acme.com ", " orders@acme.com ", "P1", " gift "),
            QrSender("Beta", "beta@x.com", "", "", "")
        )

        val result = useCase.importDecoded(items)

        assertTrue(result is ImportSendersFromQrResult.Success)
        assertEquals(2, dao.getAll().size)
        val stored = dao.getByEmail("billing@acme.com")
        assertEquals("Acme", stored?.companyName)
        assertEquals("orders@acme.com", stored?.outboundEmail)
        assertEquals("gift", stored?.skipKeywords)
    }

    @Test
    fun duplicateRows_insertedOnce() = runTest {
        useCase.importDecoded(
            listOf(
                QrSender("Acme", "a@x.com", "r@x.com", "", ""),
                QrSender("Acme dup", "A@X.COM", "R@X.COM", "", "")
            )
        )

        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun rowWithoutEmail_isSkipped() = runTest {
        useCase.importDecoded(
            listOf(
                QrSender("NoMail", "", "r@x.com", "", ""),
                QrSender("Acme", "a@x.com", "", "", "")
            )
        )

        assertEquals(listOf("a@x.com"), dao.getAll().map { it.inboundEmail })
    }

    @Test
    fun matchingExistingSender_updatesInsteadOfInserting() = runTest {
        dao.seed(SenderEntity(companyName = "Old", inboundEmail = "a@x.com", outboundEmail = "old@x.com"))

        val result = useCase.importDecoded(listOf(QrSender("New", "a@x.com", "new@x.com", "P2", "")))

        assertTrue(result is ImportSendersFromQrResult.Success)
        assertEquals(1, dao.getAll().size)
        val stored = dao.getByEmail("a@x.com")
        assertEquals("New", stored?.companyName)
        assertEquals("new@x.com", stored?.outboundEmail)
        assertEquals("P2", stored?.parser)
    }

    @Test
    fun normalize_trimsDedupesAndDropsBlankEmails() {
        val normalized = useCase.normalize(
            listOf(
                QrSender("  Acme ", " A@X.COM ", " R@X.COM ", "", ""),
                QrSender("dup", "a@x.com", "r@x.com", "", ""),
                QrSender("NoMail", "  ", "r@x.com", "", "")
            )
        )

        assertEquals(listOf(QrSender("Acme", "A@X.COM", "R@X.COM", "", "")), normalized)
    }

    private class InMemorySenderDao : SenderDao {
        private val rows = mutableListOf<SenderEntity>()
        private val flow = MutableStateFlow<List<SenderEntity>>(emptyList())

        fun seed(vararg senders: SenderEntity) {
            rows.addAll(senders)
            flow.value = rows.toList()
        }

        override fun observeAll(): Flow<List<SenderEntity>> = flow

        override suspend fun getAll(): List<SenderEntity> = rows.toList()

        override suspend fun insert(sender: SenderEntity): Long {
            rows += sender
            flow.value = rows.toList()
            return rows.size.toLong()
        }

        override suspend fun update(sender: SenderEntity) {
            val index = rows.indexOfFirst { it.inboundEmail == sender.inboundEmail }
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
        ): Int = 0

        override suspend fun getByEmail(email: String): SenderEntity? =
            rows.firstOrNull { it.inboundEmail == email }

        override suspend fun deleteByEmail(email: String): Int = 0
    }
}
