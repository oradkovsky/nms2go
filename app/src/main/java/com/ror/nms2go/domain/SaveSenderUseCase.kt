package com.ror.nms2go.domain

import android.database.sqlite.SQLiteConstraintException
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import javax.inject.Inject

sealed interface SaveSenderResult {
    data object Saved : SaveSenderResult
    data object DuplicateEmail : SaveSenderResult
}

class SaveSenderUseCase @Inject constructor(
    private val senderRepository: SenderRepository
) {
    /**
     * @param originalEmail email of the edited row, or null when adding a new sender.
     *
     * Email is the primary key. A same-key edit performs an update, anything
     * else performs an insert. A rename (new email) only inserts the new row –
     * removing the old row belongs to the caller, never to this use case.
     * Collisions are detected by the database unique constraint and mapped to
     * [SaveSenderResult.DuplicateEmail].
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
        if (company.isBlank() || normalizedEmail.isBlank()) return SaveSenderResult.Saved

        if (originalEmail != null && originalEmail == normalizedEmail) {
            val existing = senderRepository.getByEmail(originalEmail)
            if (existing != null) {
                try {
                    senderRepository.update(
                        existing.copy(
                            companyName = company,
                            email = normalizedEmail,
                            receiverEmail = receiver,
                            parser = parserValue,
                            skipKeywords = skipValue
                        )
                    )
                } catch (error: SQLiteConstraintException) {
                    return SaveSenderResult.DuplicateEmail
                }
                return SaveSenderResult.Saved
            }
        }

        val createdAt = originalEmail
            ?.let { senderRepository.getByEmail(it)?.createdAt }
            ?: System.currentTimeMillis()
        try {
            senderRepository.insert(
                SenderEntity(
                    companyName = company,
                    email = normalizedEmail,
                    receiverEmail = receiver,
                    parser = parserValue,
                    skipKeywords = skipValue,
                    createdAt = createdAt
                )
            )
        } catch (error: SQLiteConstraintException) {
            return SaveSenderResult.DuplicateEmail
        }
        return SaveSenderResult.Saved
    }
}
