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
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset

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

    const val NAV_DAMPING_RATIO =
        0.9f

    const val NAV_STIFFNESS =
        520f

    const val NAV_PARALLAX_DIVISOR =
        4

    const val NAV_DIM_ALPHA =
        0.45f

    const val NAV_FADE_IN_MS =
        140

    const val NAV_FADE_OUT_MS =
        170
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

    if (!motionEnabled) {
        content(targetState)
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
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    if (!motionEnabled) {
        content(targetState)
        return
    }

    Crossfade(
        targetState = targetState,
        animationSpec = tween(
            durationMillis = AuralArcMotion.NORMAL
        )
    ) { state ->
        content(state)
    }
}

enum class AuralArcTransitionKind {
    NONE,
    SLIDE_FORWARD,
    SLIDE_BACK,
    GROW_IN,
    GROW_OUT
}

@Composable
fun <T> AuralArcStackedTransition(
    targetState: T,
    kindFor: (initial: T, target: T) -> AuralArcTransitionKind,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable (T) -> Unit
) {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    if (!motionEnabled) {
        Box(modifier = modifier) {
            content(targetState)
        }
        return
    }

    val offsetSpec =
        spring<IntOffset>(
            dampingRatio =
                AuralArcMotion.NAV_DAMPING_RATIO,
            stiffness =
                AuralArcMotion.NAV_STIFFNESS,
            visibilityThreshold =
                IntOffset.VisibilityThreshold
        )

    val floatSpec =
        spring<Float>(
            dampingRatio =
                AuralArcMotion.NAV_DAMPING_RATIO,
            stiffness =
                AuralArcMotion.NAV_STIFFNESS
        )

    val fadeIn =
        tween<Float>(
            durationMillis =
                AuralArcMotion.NAV_FADE_IN_MS
        )

    val fadeOut =
        tween<Float>(
            durationMillis =
                AuralArcMotion.NAV_FADE_OUT_MS
        )

    AnimatedContent(
        targetState = targetState,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            val kind =
                kindFor(
                    this.initialState,
                    this.targetState
                )

            val transform =
                when (kind) {
                    AuralArcTransitionKind.NONE ->
                        fadeIn(
                            animationSpec = tween(
                                durationMillis =
                                    AuralArcMotion.NORMAL
                            )
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis =
                                    AuralArcMotion.FAST
                            )
                        )

                    AuralArcTransitionKind.SLIDE_FORWARD ->
                        (
                                slideInHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth -> fullWidth } +
                                        fadeIn(
                                            animationSpec = fadeIn
                                        )
                                ) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth ->
                                    -fullWidth /
                                            AuralArcMotion.NAV_PARALLAX_DIVISOR
                                } +
                                        fadeOut(
                                            animationSpec = fadeOut
                                        )
                                )

                    AuralArcTransitionKind.SLIDE_BACK ->
                        (
                                slideInHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth ->
                                    -fullWidth /
                                            AuralArcMotion.NAV_PARALLAX_DIVISOR
                                } +
                                        fadeIn(
                                            animationSpec = fadeIn
                                        )
                                ) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth -> fullWidth } +
                                        fadeOut(
                                            animationSpec = fadeOut
                                        )
                                )

                    AuralArcTransitionKind.GROW_IN ->
                        (
                                slideInHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth -> fullWidth / 5 } +
                                        scaleIn(
                                            animationSpec = floatSpec,
                                            initialScale = 0.90f
                                        ) +
                                        fadeIn(
                                            animationSpec = fadeIn
                                        )
                                ) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth -> -fullWidth / 8 } +
                                        scaleOut(
                                            animationSpec = floatSpec,
                                            targetScale = 0.95f
                                        ) +
                                        fadeOut(
                                            animationSpec = fadeOut
                                        )
                                )

                    AuralArcTransitionKind.GROW_OUT ->
                        (
                                slideInHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth -> -fullWidth / 8 } +
                                        scaleIn(
                                            animationSpec = floatSpec,
                                            initialScale = 0.95f
                                        ) +
                                        fadeIn(
                                            animationSpec = fadeIn
                                        )
                                ) togetherWith (
                                slideOutHorizontally(
                                    animationSpec = offsetSpec
                                ) { fullWidth -> fullWidth / 5 } +
                                        scaleOut(
                                            animationSpec = floatSpec,
                                            targetScale = 0.90f
                                        ) +
                                        fadeOut(
                                            animationSpec = fadeOut
                                        )
                                )
                }

            transform.targetContentZIndex =
                when (kind) {
                    AuralArcTransitionKind.SLIDE_FORWARD,
                    AuralArcTransitionKind.GROW_IN -> 1f

                    AuralArcTransitionKind.SLIDE_BACK,
                    AuralArcTransitionKind.GROW_OUT -> -1f

                    AuralArcTransitionKind.NONE -> 0f
                }

            transform.using(
                SizeTransform(
                    clip = true
                ) { _, _ ->
                    snap()
                }
            )
        }
    ) { state ->
        content(
            state
        )
    }
}