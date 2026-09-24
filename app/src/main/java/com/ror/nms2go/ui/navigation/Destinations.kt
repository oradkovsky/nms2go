package com.ror.nms2go.ui.navigation

import kotlinx.serialization.Serializable

object Destinations {
    const val OVERVIEW = "overview"
    const val CONFIG = "config"
    const val CONFIG_DETAIL = "config_detail?senderEmail={senderEmail}"
    const val QR_SCAN = "qr_scan"
    const val PARSED = "parsed"
    const val REVIEW = "review"
    const val ORDERS = "orders"
    val ORDER_DETAIL = "${OrderDetailRoute::class.qualifiedName}/{orderId}"
}

@Serializable
data class OrderDetailRoute(
    val orderId: Long
)
