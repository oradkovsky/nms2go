package com.ror.nms2go.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.ror.nms2go.R
import com.ror.nms2go.data.SenderEntity
import com.ror.nms2go.data.SenderOverview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Nms2GoTopBar(
    currentRoute: String,
    isEditingSender: Boolean,
    drawerState: DrawerState,
    senders: List<SenderEntity>,
    loading: Boolean,
    overviewResults: List<SenderOverview>,
    orderQuantities: Map<Int, Int>,
    onBack: () -> Unit,
    onNavigateToReview: () -> Unit,
    onOrder: () -> Unit,
    onLoad: () -> Unit
) {
    val scope = rememberCoroutineScope()

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = when (currentRoute) {
                    Destinations.CONFIG ->
                        stringResource(R.string.config_title)

                    Destinations.CONFIG_DETAIL ->
                        if (isEditingSender) {
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
                IconButton(onClick = onBack) {
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
                    onClick = onNavigateToReview,
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
                    onClick = onOrder,
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
