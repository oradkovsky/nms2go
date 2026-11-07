package com.ror.nms2go.analytics

/**
 * Single source of truth for analytics event names and their attribute keys.
 *
 * Every event exposes its name and nests all supported attributes beneath it,
 * so call sites never hardcode analytics strings.
 */
object AnalyticsEvents {

    object NavDrawer {
        const val EVENT_NAME = "ev_nav_drawer"

        object Attributes {
            const val NAME = "name"
        }
    }

    object OverviewOrderAll {
        const val EVENT_NAME = "ev_overview_order_all"

        object Attributes {
            const val ITEMS_COUNT = "items_count"
        }
    }
}
