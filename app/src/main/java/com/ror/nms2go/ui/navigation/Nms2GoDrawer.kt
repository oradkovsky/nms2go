package com.ror.nms2go.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.ror.nms2go.R
import kotlinx.coroutines.launch

@Composable
fun Nms2GoDrawerContent(
    drawerState: DrawerState,
    currentRoute: String?,
    onOverviewClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    fun closeDrawer() {
        scope.launch {
            drawerState.animateTo(
                DrawerValue.Closed,
                tween(DRAWER_ANIMATION_DURATION, easing = FastOutSlowInEasing)
            )
        }
    }

    ModalDrawerSheet {
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_overview)) },
            selected = currentRoute == Destinations.OVERVIEW,
            onClick = {
                closeDrawer()
                onOverviewClick()
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.orders_title)) },
            selected = currentRoute == Destinations.ORDERS,
            onClick = {
                closeDrawer()
                onOrdersClick()
            },
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_configuration)) },
            selected = currentRoute == Destinations.CONFIG,
            onClick = {
                closeDrawer()
                onConfigClick()
            },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
        )
    }
}
