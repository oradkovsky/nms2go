package com.ror.nms2go.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ror.nms2go.ParsedExcel
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderOverview
import com.ror.nms2go.ui.navigation.Destinations
import com.ror.nms2go.ui.navigation.Nms2GoDrawerContent
import com.ror.nms2go.ui.navigation.Nms2GoNavHost
import com.ror.nms2go.ui.navigation.Nms2GoTopBar

@Composable
fun Nms2GoApp(
    senders: List<SenderEntity>,
    loading: Boolean,
    statusText: String,
    overviewResults: List<SenderOverview>,
    onLoad: () -> Unit,
    onParseItem: (SenderOverview) -> Unit,
    onOrder: () -> Unit,
    onSendOrders: () -> Unit,
    parsedExcel: ParsedExcel?,
    onDismissParsed: () -> Unit,
    orderQuantities: Map<Int, Int>,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    orderSentStamp: Int,
    sendingOrders: Boolean,
    orderSendError: String?,
    orderLoadingProgress: Pair<Int, Int>? = null
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destinations.OVERVIEW

    val isEditingSender: Boolean =
        backStackEntry?.arguments?.getLong("senderId")?.let { it != -1L } ?: false

    LaunchedEffect(parsedExcel) {
        if (parsedExcel != null && currentRoute != Destinations.PARSED) {
            navController.navigate(Destinations.PARSED) {
                launchSingleTop = true
            }
        }
    }

    val backToOverview: () -> Unit = {
        onDismissParsed()
        navController.popBackStack(Destinations.OVERVIEW, inclusive = false)
    }

    LaunchedEffect(orderSentStamp) {
        if (orderSentStamp > 0 && currentRoute == Destinations.REVIEW) {
            onDismissParsed()
            navController.navigate(Destinations.ORDERS) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Nms2GoDrawerContent(
                drawerState = drawerState,
                currentRoute = currentRoute,
                onOverviewClick = {
                    navController.navigate(Destinations.OVERVIEW) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onOrdersClick = {
                    navController.navigate(Destinations.ORDERS) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onConfigClick = {
                    navController.navigate(Destinations.CONFIG) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Nms2GoTopBar(
                    currentRoute = currentRoute,
                    isEditingSender = isEditingSender,
                    drawerState = drawerState,
                    senders = senders,
                    loading = loading,
                    overviewResults = overviewResults,
                    orderQuantities = orderQuantities,
                    onBack = { navController.popBackStack() },
                    onNavigateToReview = {
                        navController.navigate(Destinations.REVIEW) {
                            launchSingleTop = true
                        }
                    },
                    onOrder = onOrder,
                    onLoad = onLoad
                )
            }
        ) { innerPadding ->
            Nms2GoNavHost(
                navController = navController,
                loading = loading,
                statusText = statusText,
                overviewResults = overviewResults,
                onLoad = onLoad,
                onParseItem = onParseItem,
                parsedExcel = parsedExcel,
                orderQuantities = orderQuantities,
                onQuantityChange = onQuantityChange,
                orderLoadingProgress = orderLoadingProgress,
                sendingOrders = sendingOrders,
                orderSendError = orderSendError,
                onSendOrders = onSendOrders,
                onBackToOverview = backToOverview,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
