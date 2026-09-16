package com.ror.nms2go.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ror.nms2go.ParsedExcel
import com.ror.nms2go.R
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderOverview
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

object Destinations {
    const val OVERVIEW = "overview"
    const val CONFIG = "config"
    const val CONFIG_DETAIL = "config_detail/{senderId}"
    const val QR_SCAN = "qr_scan"
    const val PARSED = "parsed"
    const val REVIEW = "review"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "order_detail/{orderId}"
}

@Serializable
data class OrderDetailRoute(
    val orderId: Long
)

private const val DRAWER_ANIMATION_DURATION = 300

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInForward(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutForward(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInBack(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutBack(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destinations.OVERVIEW

    fun editingSender(): Boolean =
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
            ModalDrawerSheet {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_overview)) },
                    selected = currentRoute == Destinations.OVERVIEW,
                    onClick = {
                        scope.launch {
                            drawerState.animateTo(
                                DrawerValue.Closed,
                                tween(DRAWER_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                            )
                        }
                        navController.navigate(Destinations.OVERVIEW) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.orders_title)) },
                    selected = currentRoute == Destinations.ORDERS,
                    onClick = {
                        scope.launch {
                            drawerState.animateTo(
                                DrawerValue.Closed,
                                tween(DRAWER_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                            )
                        }
                        navController.navigate(Destinations.ORDERS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_configuration)) },
                    selected = currentRoute == Destinations.CONFIG,
                    onClick = {
                        scope.launch {
                            drawerState.animateTo(
                                DrawerValue.Closed,
                                tween(DRAWER_ANIMATION_DURATION, easing = FastOutSlowInEasing)
                            )
                        }
                        navController.navigate(Destinations.CONFIG) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Destinations.CONFIG ->
                                    stringResource(R.string.config_title)

                                Destinations.CONFIG_DETAIL ->
                                    if (editingSender()) {
                                        stringResource(R.string.config_edit_title)
                                    } else {
                                        stringResource(R.string.config_add_title)
                                    }

                                Destinations.QR_SCAN ->
                                    stringResource(R.string.qr_scan_title)

                                Destinations.PARSED ->
                                    stringResource(R.string.parsed_title)

                                Destinations.REVIEW ->
                                    stringResource(R.string.review_title)

                                Destinations.ORDERS ->
                                    stringResource(R.string.orders_title)

                                Destinations.ORDER_DETAIL ->
                                    stringResource(R.string.order_detail_title)

                                else -> stringResource(R.string.menu_overview)
                            }
                        )
                    },
                    navigationIcon = {
                        if (currentRoute == Destinations.PARSED ||
                            currentRoute == Destinations.CONFIG_DETAIL ||
                            currentRoute == Destinations.REVIEW ||
                            currentRoute == Destinations.ORDER_DETAIL ||
                            currentRoute == Destinations.QR_SCAN
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.animateTo(
                                        DrawerValue.Open,
                                        tween(
                                            DRAWER_ANIMATION_DURATION,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = stringResource(R.string.menu_open)
                                )
                            }
                        }
                    },
                    actions = {
                        if (currentRoute == Destinations.PARSED) {
                            IconButton(
                                onClick = {
                                    navController.navigate(Destinations.REVIEW) {
                                        launchSingleTop = true
                                    }
                                },
                                enabled = orderQuantities.any { it.value > 0 }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = stringResource(R.string.review_title)
                                )
                            }
                        }
                        if (currentRoute == Destinations.OVERVIEW) {
                            IconButton(
                                onClick = {
                                    onOrder()
                                },
                                enabled = !loading && overviewResults.any {
                                    it.status == SenderOverview.Status.FOUND
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = stringResource(R.string.overview_order)
                                )
                            }
                            IconButton(
                                onClick = onLoad,
                                enabled = !loading && senders.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.overview_refresh)
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Destinations.OVERVIEW,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { slideInForward() },
                exitTransition = { slideOutForward() },
                popEnterTransition = { slideInBack() },
                popExitTransition = { slideOutBack() }
            ) {
                composable(Destinations.OVERVIEW) {
                    OverviewScreen(
                        senders = senders,
                        loading = loading,
                        statusText = statusText,
                        overviewResults = overviewResults,
                        onLoad = onLoad,
                        onParseItem = onParseItem,
                        onNavigateToConfig = {
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
                composable(Destinations.CONFIG) {
                    ConfigScreen(
                        onAdd = {
                            navController.navigate(
                                Destinations.CONFIG_DETAIL.replace(
                                    "{senderId}",
                                    "-1"
                                )
                            )
                        },
                        onEdit = { id ->
                            navController.navigate(
                                Destinations.CONFIG_DETAIL.replace(
                                    "{senderId}",
                                    "$id"
                                )
                            )
                        },
                        onScanQr = {
                            navController.navigate(Destinations.QR_SCAN)
                        }
                    )
                }
                composable(Destinations.QR_SCAN) {
                    val qrViewModel: QrScanViewModel = hiltViewModel()
                    val qrUiState by qrViewModel.uiState.collectAsState()
                    LaunchedEffect(qrViewModel) {
                        qrViewModel.uiState.collect { state ->
                            if (state is QrScanUiState.Success) {
                                navController.popBackStack()
                                qrViewModel.consumeSuccess()
                            }
                        }
                    }
                    QrScanScreen(
                        uiState = qrUiState,
                        onRawScanned = { raw -> qrViewModel.handleRawScanned(raw) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Destinations.CONFIG_DETAIL,
                    arguments = listOf(navArgument("senderId") { type = NavType.LongType })
                ) {
                    val viewModel: ConfigDetailViewModel = hiltViewModel()
                    val sender by viewModel.sender.collectAsState()
                    ConfigDetailScreen(
                        initialSender = sender,
                        onAdd = viewModel::save,
                        onUpdate = { _, company, email, receiver, parser ->
                            viewModel.save(company, email, receiver, parser)
                        },
                        onDelete = { viewModel.delete() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Destinations.PARSED) {
                    val parsedViewModel: ParsedViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel()
                    val parsedUiState by parsedViewModel.uiState.collectAsState()
                    LaunchedEffect(parsedExcel, orderQuantities, loading, orderLoadingProgress) {
                        parsedViewModel.updateData(
                            parsedExcel,
                            orderQuantities,
                            loading && orderLoadingProgress != null,
                            orderLoadingProgress
                        )
                    }
                    LaunchedEffect(parsedViewModel) {
                        parsedViewModel.quantityChangeEvent.collect { (index, qty) ->
                            onQuantityChange(index, qty)
                        }
                    }
                    ParsedScreen(
                        uiState = parsedUiState,
                        onQueryChange = parsedViewModel::onQueryChange,
                        onClearQuery = parsedViewModel::onClearQuery,
                        onPageChange = parsedViewModel::onPageChange,
                        onQuantityChange = parsedViewModel::onQuantityChange,
                        onBack = backToOverview
                    )
                }
                composable(Destinations.REVIEW) {
                    val reviewViewModel: ReviewViewModel =
                        androidx.lifecycle.viewmodel.compose.viewModel()
                    val reviewUiState by reviewViewModel.uiState.collectAsState()
                    LaunchedEffect(parsedExcel, orderQuantities, sendingOrders, orderSendError) {
                        reviewViewModel.updateData(
                            parsedExcel,
                            orderQuantities,
                            sendingOrders,
                            orderSendError
                        )
                    }
                    LaunchedEffect(reviewViewModel) {
                        reviewViewModel.quantityChangeEvent.collect { (index, qty) ->
                            onQuantityChange(index, qty)
                        }
                    }
                    LaunchedEffect(reviewViewModel) {
                        reviewViewModel.orderRequested.collect {
                            onSendOrders()
                        }
                    }
                    ReviewScreen(
                        uiState = reviewUiState,
                        onQuantityChange = reviewViewModel::onQuantityChange,
                        onOrderRequested = reviewViewModel::onOrderRequested,
                        onConfirmOrder = reviewViewModel::onConfirmOrder,
                        onDismissConfirm = reviewViewModel::onDismissConfirm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Destinations.ORDERS) {
                    OrdersHistoryScreen(
                        onOrderClick = { orderId ->
                            navController.navigate(OrderDetailRoute(orderId))
                        }
                    )
                }
                composable<OrderDetailRoute> {
                    OrderDetailScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
