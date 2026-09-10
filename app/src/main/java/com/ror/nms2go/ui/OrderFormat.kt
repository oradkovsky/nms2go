package com.ror.nms2go.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatOrderTimestamp(epochMillis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(Date(epochMillis))
}