package com.ror.nms2go.ui

import com.ror.nms2go.analytics.Analytics
import com.ror.nms2go.analytics.AnalyticsEvent
import com.ror.nms2go.data.SenderDao
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderOverview
import com.ror.nms2go.data.SenderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val sendersFlow = MutableStateFlow<List<SenderEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        senders: List<SenderEntity> = emptyList(),
        analytics: Analytics = Analytics {}
    ): OverviewViewModel {
        sendersFlow.value = senders
        return OverviewViewModel(SenderRepository(FakeSenderDao(sendersFlow)), analytics)
    }

    @Test
    fun updateData_mapsDomainObjectsToRenderableItems() {
        val viewModel = viewModel(
            senders = listOf(
                SenderEntity(
                    companyName = "Acme",
                    inboundEmail = "orders@acme.example",
                    outboundEmail = "",
                    parser = ""
                )
            )
        )

        viewModel.updateData(
            loading = false,
            statusText = "1 email loaded",
            overviewResults = listOf(
                SenderOverview(
                    senderQuery = "orders@acme.example",
                    messageId = "message-1",
                    subject = "Price list (15.01.2026)",
                    from = "Acme",
                    date = "15.01.2026 10:00",
                    hasAttachment = true,
                    status = SenderOverview.Status.FOUND
                )
            )
        )

        val model = viewModel.uiModel.value
        assertTrue(model.hasConfiguredSenders)
        assertFalse(model.isLoading)
        assertFalse(model.showEmptyState)
        assertEquals("1 email loaded", model.statusText)
        assertEquals(
            OverviewUiItem(
                senderQuery = "orders@acme.example",
                companyName = "Acme",
                subject = "Price list",
                date = "15.01.2026 10:00",
                status = OverviewUiItemStatus.FOUND
            ),
            model.items.single()
        )
    }

    @Test
    fun updateData_withoutConfiguredSenders_showsEmptyState() {
        val viewModel = viewModel()

        viewModel.updateData(
            loading = false,
            statusText = "",
            overviewResults = emptyList()
        )

        val model = viewModel.uiModel.value
        assertTrue(model.showEmptyState)
        assertFalse(model.showStatus)
        assertTrue(model.items.isEmpty())
    }

    @Test
    fun onItemClicked_withMatchingItem_logsMatchedEvent() {
        val events = mutableListOf<AnalyticsEvent>()
        val viewModel = viewModel(analytics = Analytics { events += it })
        viewModel.updateData(
            loading = false,
            statusText = "",
            overviewResults = listOf(foundOverview("orders@acme.example"))
        )

        viewModel.onItemClicked("orders@acme.example")

        val event = events.single()
        assertEquals("ev_overview_order_single", event.name)
        assertEquals(true, event.attributes["matched"])
        assertEquals("orders@acme.example", event.attributes["query"])
    }

    @Test
    fun onItemClicked_withoutMatchingItem_logsUnmatchedEvent() {
        val events = mutableListOf<AnalyticsEvent>()
        val viewModel = viewModel(analytics = Analytics { events += it })
        viewModel.updateData(
            loading = false,
            statusText = "",
            overviewResults = listOf(foundOverview("orders@acme.example"))
        )

        viewModel.onItemClicked("missing@acme.example")

        val event = events.single()
        assertEquals("ev_overview_order_single", event.name)
        assertEquals(false, event.attributes["matched"])
        assertEquals("missing@acme.example", event.attributes["query"])
    }

    private fun foundOverview(senderQuery: String) = SenderOverview(
        senderQuery = senderQuery,
        messageId = "message-1",
        subject = "Price list",
        from = "Acme",
        date = "15.01.2026 10:00",
        hasAttachment = true,
        status = SenderOverview.Status.FOUND
    )

    private class FakeSenderDao(
        private val flow: MutableStateFlow<List<SenderEntity>>
    ) : SenderDao {
        override fun observeAll(): Flow<List<SenderEntity>> = flow

        override suspend fun getAll(): List<SenderEntity> = flow.value

        override suspend fun insert(sender: SenderEntity): Long =
            throw UnsupportedOperationException()

        override suspend fun update(sender: SenderEntity): Unit =
            throw UnsupportedOperationException()

        override suspend fun updateByEmail(
            originalEmail: String,
            email: String,
            companyName: String,
            receiverEmail: String,
            parser: String,
            skipKeywords: String
        ): Int = throw UnsupportedOperationException()

        override suspend fun getByEmail(email: String): SenderEntity? =
            flow.value.firstOrNull { it.inboundEmail == email }

        override suspend fun deleteByEmail(email: String): Int =
            throw UnsupportedOperationException()
    }
}
