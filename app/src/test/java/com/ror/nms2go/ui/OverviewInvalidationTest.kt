package com.ror.nms2go.ui

import com.ror.nms2go.data.SenderEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewInvalidationTest {

    private fun sender(email: String) = SenderEntity(
        companyName = "Company $email",
        inboundEmail = email
    )

    @Test
    fun noSnapshot_neverInvalidates() {
        val latest = listOf(sender("a@example.com"))

        assertFalse(shouldInvalidateOverview(null, latest, hasResults = true))
        assertFalse(shouldInvalidateOverview(null, latest, hasResults = false))
        assertFalse(shouldInvalidateOverview(null, emptyList(), hasResults = true))
    }

    @Test
    fun unchangedSenders_neverInvalidates() {
        val senders = listOf(sender("a@example.com"), sender("b@example.com"))

        assertFalse(shouldInvalidateOverview(senders, senders.toList(), hasResults = true))
    }

    @Test
    fun editedSender_invalidatesWhenResultsPresent() {
        val loadedFor = listOf(sender("a@example.com"))
        val latest = listOf(sender("a-changed@example.com"))

        assertTrue(shouldInvalidateOverview(loadedFor, latest, hasResults = true))
    }

    @Test
    fun addedSender_invalidatesWhenResultsPresent() {
        val loadedFor = listOf(sender("a@example.com"))
        val latest = loadedFor + sender("b@example.com")

        assertTrue(shouldInvalidateOverview(loadedFor, latest, hasResults = true))
    }

    @Test
    fun deletedSender_invalidatesWhenResultsPresent() {
        val loadedFor = listOf(sender("a@example.com"), sender("b@example.com"))
        val latest = listOf(sender("a@example.com"))

        assertTrue(shouldInvalidateOverview(loadedFor, latest, hasResults = true))
    }

    @Test
    fun changedSenders_noResults_nothingToInvalidate() {
        val loadedFor = listOf(sender("a@example.com"))
        val latest = listOf(sender("a-changed@example.com"))

        assertFalse(shouldInvalidateOverview(loadedFor, latest, hasResults = false))
    }
}
