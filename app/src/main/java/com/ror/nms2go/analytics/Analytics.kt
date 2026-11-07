package com.ror.nms2go.analytics

fun interface Analytics {
    fun logEvent(name: String, vararg params: Pair<String, Any?>)
}
