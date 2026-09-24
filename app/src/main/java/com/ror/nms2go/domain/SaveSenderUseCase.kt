package com.ror.nms2go.domain

import android.database.sqlite.SQLiteConstraintException
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import javax.inject.Inject

sealed interface SaveSenderResult {
    data object Saved : SaveSenderResult
    data object DuplicateEmail : SaveSenderResult
    data object InvalidInput : SaveSenderResult
    data class StorageError(val cause: Throwable) : SaveSenderResult
}

class SaveSenderUseCase @Inject constructor(
    private val senderRepository: SenderRepository
) {
    /**
     * @param inboundEmailBeforeChange email of the edited row, or null when adding a new sender.
     *
     * Insert-or-update only, never delete. A same-key edit and a rename
     * (email change) are both a single update; the database reports
     * constraint violations, which are classified afterwards: a conflicting
     * row under the new email maps to [SaveSenderResult.DuplicateEmail],
     * any other constraint failure to [SaveSenderResult.StorageError].
     * Blank company or email is rejected with
     * [SaveSenderResult.InvalidInput] – both fields are required.
     */
    suspend operator fun invoke(
        inboundEmailBeforeChange: String?,
        companyName: String,
        inboundEmail: String,
        outboundEmail: String,
        parser: String,
        skipKeywords: String = ""
    ): SaveSenderResult {
        val company = companyName.trim()
        val normalizedEmail = inboundEmail.trim()
        val receiver = outboundEmail.trim()
        val parserValue = parser.trim()
        val skipValue = skipKeywords.trim()
        if (company.isBlank() || normalizedEmail.isBlank()) return SaveSenderResult.InvalidInput

        try {
            if (inboundEmailBeforeChange == null) {
                senderRepository.insert(
                    SenderEntity(
                        companyName = company,
                        inboundEmail = normalizedEmail,
                        outboundEmail = receiver,
                        parser = parserValue,
                        skipKeywords = skipValue
                    )
                )
            } else {
                senderRepository.updateByEmail(
                    originalEmail = inboundEmailBeforeChange,
                    email = normalizedEmail,
                    companyName = company,
                    receiverEmail = receiver,
                    parser = parserValue,
                    skipKeywords = skipValue
                )
            }
        } catch (error: SQLiteConstraintException) {
            // A constraint violation is not necessarily the email key –
            // confirm a conflicting row before reporting a duplicate.
            val conflicting = senderRepository.getByEmail(normalizedEmail)
            return if (conflicting != null && conflicting.inboundEmail != inboundEmailBeforeChange) {
                SaveSenderResult.DuplicateEmail
            } else {
                SaveSenderResult.StorageError(error)
            }
        }
        return SaveSenderResult.Saved
    }
}
