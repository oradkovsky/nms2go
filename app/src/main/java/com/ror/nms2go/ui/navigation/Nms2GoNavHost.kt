package com.ror.nms2go.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ror.nms2go.ParsedExcel
import com.ror.nms2go.data.SenderOverview
import com.ror.nms2go.ui.ConfigDetailScreen
import com.ror.nms2go.ui.ConfigDetailViewModel
import com.ror.nms2go.ui.ConfigScreen
import com.ror.nms2go.ui.OrderDetailScreen
import com.ror.nms2go.ui.OrdersHistoryScreen
import com.ror.nms2go.ui.OverviewScreen
import com.ror.nms2go.ui.OverviewViewModel
import com.ror.nms2go.ui.ParsedScreen
import com.ror.nms2go.ui.ParsedViewModel
import com.ror.nms2go.ui.QrScanScreen
import com.ror.nms2go.ui.QrScanUiState
import com.ror.nms2go.ui.QrScanViewModel
import com.ror.nms2go.ui.ReviewScreen
import com.ror.nms2go.ui.ReviewViewModel

@Composable
fun Nms2GoNavHost(
    navController: NavHostController,
    loading: Boolean,
    statusText: String,
    overviewResults: List<SenderOverview>,
    onLoad: () -> Unit,
    onParseItem: (SenderOverview) -> Unit,
    parsedExcel: ParsedExcel?,
    orderQuantities: Map<Int, Int>,
    onQuantityChange: (index: Int, quantity: Int) -> Unit,
    orderLoadingProgress: Pair<Int, Int>?,
    sendingOrders: Boolean,
    orderSendError: String?,
    onSendOrders: () -> Unit,
    onBackToOverview: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.OVERVIEW,
        modifier = modifier,
        enterTransition = { slideInForward() },
        exitTransition = { slideOutForward() },
        popEnterTransition = { slideInBack() },
        popExitTransition = { slideOutBack() }
    ) {
        composable(Destinations.OVERVIEW) {
            val overviewViewModel: OverviewViewModel = hiltViewModel()
            val overviewUiModel by overviewViewModel.uiModel.collectAsState()

            LaunchedEffect(loading, statusText, overviewResults) {
                overviewViewModel.updateData(loading, statusText, overviewResults)
            }
            LaunchedEffect(overviewViewModel) {
                overviewViewModel.loadRequests.collect { onLoad() }
            }
            LaunchedEffect(overviewViewModel) {
                overviewViewModel.parseItemRequests.collect(onParseItem)
            }
            OverviewScreen(
                uiModel = overviewUiModel,
                onParseItem = overviewViewModel::onItemClicked,
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
                    navController.navigate("config_detail")
                },
                onEdit = { senderEmail ->
                    navController.navigate(
                        "config_detail?${DestinationArgs.SENDER_EMAIL}=${Uri.encode(senderEmail)}"
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
            arguments = listOf(
                navArgument(DestinationArgs.SENDER_EMAIL) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel: ConfigDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ConfigDetailScreen(
                uiState = uiState,
                onAdd = viewModel::save,
                onUpdate = { _, company, email, receiver, parser, skipKeywords ->
                    viewModel.save(company, email, receiver, parser, skipKeywords)
                },
                onDelete = { email -> viewModel.delete(email) },
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
                onBack = onBackToOverview
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
