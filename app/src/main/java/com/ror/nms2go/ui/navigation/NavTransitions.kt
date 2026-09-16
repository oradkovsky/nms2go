package com.ror.nms2go.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

internal const val DRAWER_ANIMATION_DURATION = 300

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInForward(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutForward(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideInBack(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOutBack(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(
            durationMillis = DRAWER_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )
