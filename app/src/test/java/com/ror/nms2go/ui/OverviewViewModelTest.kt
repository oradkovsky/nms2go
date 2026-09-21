package com.ror.nms2go.ui

import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewViewModelTest {

    @Test
    fun updateData_mapsDomainObjectsToRenderableItems() {
        val viewModel = OverviewViewModel()

        viewModel.updateData(
            senders = listOf(
                SenderEntity(
                    id = 1,
                    companyName = "Acme",
                    email = "orders@acme.example",
                    receiverEmail = "",
                    parser = ""
                )
            ),
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
        val viewModel = OverviewViewModel()

        viewModel.updateData(
            senders = emptyList(),
            loading = false,
            statusText = "",
            overviewResults = emptyList()
        )

        val model = viewModel.uiModel.value
        assertTrue(model.showEmptyState)
        assertFalse(model.showStatus)
        assertTrue(model.items.isEmpty())
    }
}
