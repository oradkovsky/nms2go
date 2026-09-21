package com.ror.nms2go.domain

import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import javax.inject.Inject

class SaveSenderUseCase @Inject constructor(
    private val senderRepository: SenderRepository
) {
    suspend operator fun invoke(
        senderId: Long,
        companyName: String,
        email: String,
        receiverEmail: String,
        parser: String,
        skipKeywords: String = ""
    ) {
        val company = companyName.trim()
        val normalizedEmail = email.trim()
        val receiver = receiverEmail.trim()
        val parserValue = parser.trim()
        val skipValue = skipKeywords.trim()
        if (company.isBlank() || normalizedEmail.isBlank()) return

        if (senderId != -1L) {
            val existing = senderRepository.getById(senderId) ?: return
            senderRepository.update(
                existing.copy(
                    companyName = company,
                    email = normalizedEmail,
                    receiverEmail = receiver,
                    parser = parserValue,
                    skipKeywords = skipValue
                )
            )
        } else {
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
}
