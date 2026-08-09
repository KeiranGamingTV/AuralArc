package com.keiranhaas.auralarc.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.keiranhaas.auralarc.storage.AppearancePreferences

object AuralArcMotion {

    const val FAST =
        90

    const val NORMAL =
        150

    const val SLOW =
        220

    const val PAGE =
        260
}

enum class AuralArcPageDirection {
    FORWARD,
    BACKWARD
}

@Composable
fun <T> AuralArcPageTransition(
    targetState: T,
    directionForTransition:
        (
        initialState: T,
        targetState: T
    ) -> AuralArcPageDirection = { _, _ ->
        AuralArcPageDirection.FORWARD
    },
    content: @Composable (T) -> Unit
) {
    val context =
        LocalContext.current

    AppearancePreferences.initializePageAnimations(
        context
    )

    val animationsEnabled by
    AppearancePreferences.pageAnimationsEnabledState

    if (
        !animationsEnabled
    ) {
        Crossfade(
            targetState = targetState,
            animationSpec = tween(
                durationMillis =
                    AuralArcMotion.NORMAL,
                easing =
                    FastOutSlowInEasing
            )
        ) { state ->
            content(
                state
            )
        }

        return
    }

    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            val direction =
                directionForTransition(
                    initialState,
                    targetState
                )

            val entering =
                if (
                    direction ==
                    AuralArcPageDirection.FORWARD
                ) {
                    slideInHorizontally(
                        initialOffsetX = { width ->
                            width / 4
                        },
                        animationSpec = tween(
                            durationMillis =
                                AuralArcMotion.PAGE,
                            easing =
                                FastOutSlowInEasing
                        )
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { width ->
                            -width / 4
                        },
                        animationSpec = tween(
                            durationMillis =
                                AuralArcMotion.PAGE,
                            easing =
                                FastOutSlowInEasing
                        )
                    )
                }

            val exiting =
                if (
                    direction ==
                    AuralArcPageDirection.FORWARD
                ) {
                    slideOutHorizontally(
                        targetOffsetX = { width ->
                            -width / 4
                        },
                        animationSpec = tween(
                            durationMillis =
                                AuralArcMotion.PAGE,
                            easing =
                                FastOutSlowInEasing
                        )
                    )
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { width ->
                            width / 4
                        },
                        animationSpec = tween(
                            durationMillis =
                                AuralArcMotion.PAGE,
                            easing =
                                FastOutSlowInEasing
                        )
                    )
                }

            (
                    entering +
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis =
                                        AuralArcMotion.NORMAL
                                )
                            )
                    ).togetherWith(
                    exiting +
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis =
                                        AuralArcMotion.NORMAL
                                )
                            )
                )
        }
    ) { state ->
        content(
            state
        )
    }
}

/*
 * Keep this for anything that specifically wants
 * a fade regardless of the global page-motion setting.
 */
@Composable
fun <T> AuralArcCrossfade(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        animationSpec = tween(
            durationMillis =
                AuralArcMotion.NORMAL,
            easing =
                FastOutSlowInEasing
        )
    ) { state ->
        content(
            state
        )
    }
}