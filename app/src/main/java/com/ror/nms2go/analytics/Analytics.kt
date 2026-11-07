package com.ror.nms2go.analytics

fun interface Analytics {
    fun logEvent(event: AnalyticsEvent)
}

class AnalyticsEvent(
    val name: String,
    vararg params: Pair<String, Any?>
) {
    val attributes: Map<String, Any?> = params.toMap()
}
