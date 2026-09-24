package com.ror.nms2go.ui.navigation

import kotlinx.serialization.Serializable

object DestinationArgs {
    const val SENDER_EMAIL = "senderEmail"
    const val ORDER_ID = "orderId"
}

object Destinations {
    const val OVERVIEW = "overview"
    const val CONFIG = "config"
    const val CONFIG_DETAIL =
        "config_detail?${DestinationArgs.SENDER_EMAIL}={${DestinationArgs.SENDER_EMAIL}}"
    const val QR_SCAN = "qr_scan"
    const val PARSED = "parsed"
    const val REVIEW = "review"
    const val ORDERS = "orders"
    val ORDER_DETAIL = "${OrderDetailRoute::class.qualifiedName}/{${DestinationArgs.ORDER_ID}}"
}

@Serializable
data class OrderDetailRoute(
    val orderId: Long
)
