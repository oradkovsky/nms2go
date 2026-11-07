package com.ror.nms2go.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ror.nms2go.analytics.Analytics
import com.ror.nms2go.analytics.AnalyticsEvent

enum class DrawerDestination(
    val analyticsName: String,
    val route: String
) {
    Overview("overview", Destinations.OVERVIEW),
    Orders("sent", Destinations.ORDERS),
    Configuration("configuration", Destinations.CONFIG)
}

/** Owns navigation behavior initiated from the app drawer. */
class AppNavigator(
    private val navController: NavController,
    private val analytics: Analytics
) {
    fun navigateFromDrawer(destination: DrawerDestination) {
        analytics.logEvent(
            AnalyticsEvent(EVENT_NAV_DRAWER, ATTR_NAME to destination.analyticsName)
        )

        navController.navigate(destination.route) {
            if (destination == DrawerDestination.Overview) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            } else {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                restoreState = true
            }
            launchSingleTop = true
        }
    }

    private companion object {
        const val EVENT_NAV_DRAWER = "ev_nav_drawer"
        const val ATTR_NAME = "name"
    }
}
