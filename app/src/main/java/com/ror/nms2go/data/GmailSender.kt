package com.ror.nms2go.data

/**
 * Sending half of Gmail access, behind an interface so UI tests can fake
 * delivery without network. Loading and attachment download stay on
 * [GmailRepository] directly.
 */
interface GmailSender {
    fun sendMessage(to: String, subject: String, htmlBody: String, accessToken: String)
}
