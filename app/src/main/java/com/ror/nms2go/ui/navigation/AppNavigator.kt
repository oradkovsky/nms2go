package com.ror.nms2go.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ror.nms2go.analytics.Analytics
import com.ror.nms2go.analytics.AnalyticsEvents

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
            AnalyticsEvents.NavDrawer.EVENT_NAME,
            AnalyticsEvents.NavDrawer.Attributes.NAME to destination.analyticsName
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
}
