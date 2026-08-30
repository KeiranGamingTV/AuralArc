package com.keiranhaas.auralarc.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.keiranhaas.auralarc.storage.AppearancePreferences

object AuralArcMotion {

    const val MORPH =
        180

    /*
     * General timing values.
     *
     * Most size/movement animations now use springs instead
     * of fixed tweens, but these remain useful for fades.
     */
    const val FAST =
        120

    const val NORMAL =
        200

    const val SLOW =
        320

    const val PAGE =
        340

    /*
     * Global spring behavior.
     *
     * This is slightly softer than a standard Material spring
     * so resizing feels fluid without looking bouncy.
     */
    const val DAMPING_RATIO =
        0.92f

    const val STIFFNESS =
        600f

    /*
     * Content transforms.
     *
     * 0.90 is intentionally much more noticeable than the
     * old 0.985 scale.
     */
    const val CONTENT_ENTER_SCALE =
        0.90f

    const val CONTENT_EXIT_SCALE =
        0.96f

    /*
     * Touch feedback scales.
     */
    const val CARD_PRESS_SCALE =
        0.97f

    const val BUTTON_PRESS_SCALE =
        0.94f
}

@Composable
fun rememberAuralArcMotionEnabled(): Boolean {
    val context =
        LocalContext.current

    AppearancePreferences.initializePageAnimations(
        context
    )

    val enabled by
    AppearancePreferences.pageAnimationsEnabledState

    return enabled
}

/*
 * Main fluid content transition.
 *
 * Unlike the old crossfade-heavy transition, this:
 *
 * 1. Grows the incoming content from 90% -> 100%.
 * 2. Shrinks the outgoing content from 100% -> 96%.
 * 3. Animates the measured container size between states.
 * 4. Uses only a short fade to hide compositing overlap.
 *
 * SizeTransform is especially important for components whose
 * old and new states have different dimensions.
 */
@Composable
fun <T> AuralArcContentTransition(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    if (
        !motionEnabled
    ) {
        Crossfade(
            targetState = targetState,
            animationSpec = tween(
                durationMillis =
                    AuralArcMotion.NORMAL
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
            val enter =
                scaleIn(
                    initialScale =
                        AuralArcMotion.CONTENT_ENTER_SCALE,
                    animationSpec = spring(
                        dampingRatio =
                            AuralArcMotion.DAMPING_RATIO,
                        stiffness =
                            AuralArcMotion.STIFFNESS
                    )
                ) +
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 90
                            )
                        )

            val exit =
                scaleOut(
                    targetScale =
                        AuralArcMotion.CONTENT_EXIT_SCALE,
                    animationSpec = spring(
                        dampingRatio =
                            AuralArcMotion.DAMPING_RATIO,
                        stiffness =
                            AuralArcMotion.STIFFNESS
                    )
                ) +
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 110
                            )
                        )

            enter
                .togetherWith(
                    exit
                )
                .using(
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ ->
                            spring<IntSize>(
                                dampingRatio =
                                    AuralArcMotion.DAMPING_RATIO,
                                stiffness =
                                    AuralArcMotion.STIFFNESS
                            )
                        }
                    )
                )
        }
    ) { state ->
        content(
            state
        )
    }
}

@Composable
fun <T> AuralArcCrossfade(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        animationSpec = tween(
            durationMillis =
                AuralArcMotion.NORMAL
        )
    ) { state ->
        content(
            state
        )
    }
}