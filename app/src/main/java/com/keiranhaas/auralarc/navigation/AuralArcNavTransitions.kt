package com.keiranhaas.auralarc.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import com.keiranhaas.auralarc.ui.theme.AuralArcMotion
import com.keiranhaas.auralarc.ui.theme.MiniPlayerMorphState

private fun navOffsetSpring() =
    spring(
        dampingRatio =
            AuralArcMotion.NAV_DAMPING_RATIO,
        stiffness =
            AuralArcMotion.NAV_STIFFNESS,
        visibilityThreshold =
            IntOffset.VisibilityThreshold
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.targetIsNowPlaying(): Boolean =
    targetState.destination.route ==
            Screen.NowPlaying.route

private fun AnimatedContentTransitionScope<NavBackStackEntry>.initialIsNowPlaying(): Boolean =
    initialState.destination.route ==
            Screen.NowPlaying.route

private fun reducedEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis =
                AuralArcMotion.NORMAL
        )
    )

private fun reducedExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis =
                AuralArcMotion.NORMAL
        )
    )

fun AnimatedContentTransitionScope<NavBackStackEntry>.auralArcEnter(
    motionEnabled: Boolean
): EnterTransition {
    if (!motionEnabled) {
        return reducedEnter()
    }

    return if (
        targetIsNowPlaying()
    ) {
        if (
            MiniPlayerMorphState.armed
        ) {
            /*
             * Opened from the mini player: just fade in. The album
             * art does the moving (see flyFromMiniPlayerArt).
             */
            fadeIn(
                animationSpec = tween(
                    durationMillis = 200
                )
            )
        } else {
            slideIntoContainer(
                towards = SlideDirection.Up,
                animationSpec = navOffsetSpring()
            )
        }
    } else {
        slideIntoContainer(
            towards = SlideDirection.Left,
            animationSpec = navOffsetSpring()
        ) +
                fadeIn(
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NAV_FADE_IN_MS
                    )
                )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.auralArcExit(
    motionEnabled: Boolean
): ExitTransition {
    if (!motionEnabled) {
        return reducedExit()
    }

    return if (
        targetIsNowPlaying()
    ) {
        /*
         * The screen under Now Playing stays put and only dims a
         * little while Now Playing rises over it.
         */
        fadeOut(
            targetAlpha = 0.7f,
            animationSpec = tween(
                durationMillis =
                    AuralArcMotion.NAV_FADE_OUT_MS
            )
        )
    } else {
        slideOutOfContainer(
            towards = SlideDirection.Left,
            animationSpec = navOffsetSpring(),
            targetOffset = { fullWidth ->
                fullWidth /
                        AuralArcMotion.NAV_PARALLAX_DIVISOR
            }
        ) +
                fadeOut(
                    targetAlpha =
                        AuralArcMotion.NAV_DIM_ALPHA,
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NAV_FADE_OUT_MS
                    )
                )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.auralArcPopEnter(
    motionEnabled: Boolean
): EnterTransition {
    if (!motionEnabled) {
        return reducedEnter()
    }

    return if (
        initialIsNowPlaying()
    ) {
        fadeIn(
            initialAlpha = 0.7f,
            animationSpec = tween(
                durationMillis =
                    AuralArcMotion.NAV_FADE_OUT_MS
            )
        )
    } else {
        slideIntoContainer(
            towards = SlideDirection.Right,
            animationSpec = navOffsetSpring(),
            initialOffset = { fullWidth ->
                fullWidth /
                        AuralArcMotion.NAV_PARALLAX_DIVISOR
            }
        ) +
                fadeIn(
                    initialAlpha =
                        AuralArcMotion.NAV_DIM_ALPHA,
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NAV_FADE_OUT_MS
                    )
                )
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.auralArcPopExit(
    motionEnabled: Boolean
): ExitTransition {
    if (!motionEnabled) {
        return reducedExit()
    }

    return if (
        initialIsNowPlaying()
    ) {
        if (
            targetState.destination.route ==
            Screen.Library.route
        ) {
            fadeOut(
                animationSpec = tween(
                    durationMillis = 200
                )
            )
        } else {
            slideOutOfContainer(
                towards = SlideDirection.Down,
                animationSpec = navOffsetSpring()
            )
        }
    } else {
        slideOutOfContainer(
            towards = SlideDirection.Right,
            animationSpec = navOffsetSpring()
        ) +
                fadeOut(
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NAV_FADE_OUT_MS,
                        delayMillis = 60
                    )
                )
    }
}