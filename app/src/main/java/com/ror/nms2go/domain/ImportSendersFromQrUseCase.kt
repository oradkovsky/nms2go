package com.ror.nms2go.domain

import com.ror.nms2go.data.QrCodec
import com.ror.nms2go.data.QrSender
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderRepository
import javax.inject.Inject

sealed interface ImportSendersFromQrResult {
    data class Success(val items: List<QrSender>) : ImportSendersFromQrResult
    data object InvalidQr : ImportSendersFromQrResult
}

class ImportSendersFromQrUseCase @Inject constructor(
    private val senderRepository: SenderRepository
) {
    suspend operator fun invoke(raw: String): ImportSendersFromQrResult {
        val decoded = QrCodec.decode(raw)
        if (decoded.isNullOrEmpty()) return ImportSendersFromQrResult.InvalidQr
        return importDecoded(decoded)
    }

    /**
     * Imports already-decoded items. Split from [invoke] so the
     * normalize/match/persist logic is unit-testable on the JVM –
     * `QrCodec` uses `org.json`, which is unavailable in local unit tests.
     */
    suspend fun importDecoded(decoded: List<QrSender>): ImportSendersFromQrResult {
        persist(normalize(decoded))
        return ImportSendersFromQrResult.Success(decoded)
    }

    /**
     * Trims fields, drops rows without an email, dedupes by
     * `email|receiver` (case-insensitive) keeping first occurrence.
     */
    internal fun normalize(items: List<QrSender>): List<QrSender> {
        val seenKeys = mutableSetOf<String>()
        return items.map { item ->
            item.copy(
                company = item.company.trim(),
                email = item.email.trim(),
                receiver = item.receiver.trim(),
                parser = item.parser.trim(),
                skipKeywords = item.skipKeywords.trim()
            )
        }.filter { item ->
            if (item.email.isBlank()) return@filter false
            seenKeys.add("${item.email.lowercase()}|${item.receiver.lowercase()}")
        }
    }

    internal fun findMatch(
        item: QrSender,
        existing: List<SenderEntity>,
        handledEmails: Set<String>
    ): SenderEntity? = existing.firstOrNull { sender ->
        sender.inboundEmail !in handledEmails && (
                sender.inboundEmail.equals(item.email, ignoreCase = true) ||
                        (item.receiver.isNotBlank() && sender.outboundEmail.equals(
                            item.receiver,
                            ignoreCase = true
                        ))
                )
    }

    private suspend fun persist(items: List<QrSender>) {
        val existing = senderRepository.getAll()
        val handledEmails = mutableSetOf<String>()
        for (item in items) {
            val match = findMatch(item, existing, handledEmails)
            if (match != null) {
                handledEmails += match.inboundEmail
                senderRepository.update(
                    match.copy(
                        companyName = item.company,
                        inboundEmail = item.email,
                        outboundEmail = item.receiver,
                        parser = item.parser,
                        skipKeywords = item.skipKeywords
                    )
                )
            } else {
                senderRepository.insert(
                    SenderEntity(
                        companyName = item.company,
                        inboundEmail = item.email,
                        outboundEmail = item.receiver,
                        parser = item.parser,
                        skipKeywords = item.skipKeywords
                    )
                )
            }
        }
    }
}
