package com.ror.nms2go

import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale

object EmailDateParser {
    private val overviewOutputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME

    private val fallbackPatterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z (z)",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss z",
        "dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss",
        "EEE MMM dd HH:mm:ss yyyy Z",
        "EEE MMM dd HH:mm:ss yyyy"
    )

    /**
     * For overview: returns `dd.MM.yyyy HH:mm` in device locale/timezone – the actual price date/time.
     * Prefers Gmail `internalDate` (epoch millis when Gmail received the message) as it is the
     * authoritative price time; falls back to parsing the `Date` header. Never falls back to `Date()`.
     */
    fun formatForOverview(dateHeader: String?, internalDateMillis: Long?): String? {
        // Prefer internalDate – it's the exact time Gmail received the price email
        if (internalDateMillis != null) {
            return overviewOutputFormat.format(Date(internalDateMillis))
        }
        return formatEmailDateInternal(dateHeader, overviewOutputFormat)
    }

    private fun formatEmailDateInternal(
        dateHeader: String?,
        targetFormat: SimpleDateFormat
    ): String? {
        if (dateHeader.isNullOrBlank()) return null
        val trimmed = dateHeader.trim()
        // Try java.time RFC1123 first (handles most Gmail dates)
        try {
            val zdt = ZonedDateTime.parse(trimmed, rfc1123)
            return targetFormat.format(Date.from(zdt.toInstant()))
        } catch (_: DateTimeParseException) {
            // fall through
        } catch (_: Exception) {
        }
        // Remove comments like " (UTC)" -> keep for pattern that includes it
        // Try SimpleDateFormat fallbacks with ENGLISH locale (month names are English)
        for (pattern in fallbackPatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.isLenient = true
                val date = sdf.parse(trimmed) ?: continue
                return targetFormat.format(date)
            } catch (_: Exception) {
            }
        }
        // Last resort: try to remove day-of-week if mismatched (Gmail sometimes has wrong DoW)
        // e.g., "Tue, 12 Aug 2025" where DoW doesn't match date -> parsing fails strictly.
        // Strip leading "EEE, " and try again.
        val withoutDow = trimmed.substringAfter(", ", trimmed)
        if (withoutDow != trimmed) {
            for (pattern in listOf(
                "dd MMM yyyy HH:mm:ss Z",
                "d MMM yyyy HH:mm:ss Z",
                "dd MMM yyyy HH:mm:ss z"
            )) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                    sdf.isLenient = true
                    val date = sdf.parse(withoutDow) ?: continue
                    return targetFormat.format(date)
                } catch (_: Exception) {
                }
            }
        }
        return null
    }
}
