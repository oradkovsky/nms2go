package com.ror.nms2go.data

import android.content.Context
import com.ror.nms2go.GmailAttachmentPart
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val client = GmailApiClient(File(context.filesDir, "gmail-attachments"))

    fun loadOverview(senders: List<String>, accessToken: String): List<SenderOverview> =
        client.loadOverview(senders, accessToken)

    fun downloadAttachment(
        messageId: String,
        attachment: GmailAttachmentPart,
        accessToken: String
    ): File = client.downloadAttachment(messageId, attachment, accessToken)

    fun sendMessage(to: String, subject: String, htmlBody: String, accessToken: String) {
        client.sendMessage(to, subject, htmlBody, accessToken)
    }
}
