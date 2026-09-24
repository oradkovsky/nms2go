package com.ror.nms2go.domain

import android.database.sqlite.SQLiteConstraintException
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import javax.inject.Inject

sealed interface SaveSenderResult {
    data object Saved : SaveSenderResult
    data object DuplicateEmail : SaveSenderResult
    data object InvalidInput : SaveSenderResult
}

class SaveSenderUseCase @Inject constructor(
    private val senderRepository: SenderRepository
) {
    /**
     * @param originalEmail email of the edited row, or null when adding a new sender.
     *
     * Insert-or-update only, never delete. A same-key edit and a rename
     * (email change) are both a single update; collisions are detected by
     * the database primary key constraint and mapped to
     * [SaveSenderResult.DuplicateEmail]. Blank company or email is rejected
     * with [SaveSenderResult.InvalidInput] – both fields are required.
     */
    suspend operator fun invoke(
        originalEmail: String?,
        companyName: String,
        email: String,
        receiverEmail: String,
        parser: String,
        skipKeywords: String = ""
    ): SaveSenderResult {
        val company = companyName.trim()
        val normalizedEmail = email.trim()
        val receiver = receiverEmail.trim()
        val parserValue = parser.trim()
        val skipValue = skipKeywords.trim()
        if (company.isBlank() || normalizedEmail.isBlank()) return SaveSenderResult.InvalidInput

        try {
            if (originalEmail == null) {
                senderRepository.insert(
                    SenderEntity(
                        companyName = company,
                        email = normalizedEmail,
                        receiverEmail = receiver,
                        parser = parserValue,
                        skipKeywords = skipValue
                    )
                )
            } else {
                val updated = senderRepository.updateByEmail(
                    originalEmail = originalEmail,
                    email = normalizedEmail,
                    companyName = company,
                    receiverEmail = receiver,
                    parser = parserValue,
                    skipKeywords = skipValue
                )
                if (updated == 0) {
                    // Original row is gone (deleted concurrently) – store as new.
                    senderRepository.insert(
                        SenderEntity(
                            companyName = company,
                            email = normalizedEmail,
                            receiverEmail = receiver,
                            parser = parserValue,
                            skipKeywords = skipValue
                        )
                    )
                }
            }
        } catch (error: SQLiteConstraintException) {
            return SaveSenderResult.DuplicateEmail
        }
        return SaveSenderResult.Saved
    }
}
