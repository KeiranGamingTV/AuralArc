package com.keiranhaas.auralarc.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.util.lerp

/**
 * Shares the mini player's album art position with the Now Playing
 * screen so the art can fly from one to the other.
 */
object MiniPlayerMorphState {

    @Volatile
    var miniArtBounds: Rect? =
        null

    /*
     * True only between the mini player being tapped and Now Playing
     * finishing its entrance. Navigation reads it to choose a fade
     * instead of the slide-up.
     */
    @Volatile
    var armed: Boolean =
        false

    fun armFromMiniPlayer(
        motionEnabled: Boolean
    ) {
        armed =
            motionEnabled &&
                    miniArtBounds != null
    }
}

/**
 * Put this on the mini player's album art so its position is known.
 */
@Composable
fun Modifier.captureMiniPlayerArt(): Modifier {
    DisposableEffect(
        Unit
    ) {
        onDispose {
            MiniPlayerMorphState.miniArtBounds =
                null
        }
    }

    return this.onGloballyPositioned { coordinates ->
        MiniPlayerMorphState.miniArtBounds =
            coordinates.boundsInRoot()
    }
}

/**
 * Put this on the Now Playing album art. When the screen was opened
 * from the mini player, the art starts at the mini player's size and
 * position and springs into place.
 */
@Composable
fun Modifier.flyFromMiniPlayerArt(): Modifier {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val start =
        remember {
            if (
                MiniPlayerMorphState.armed &&
                motionEnabled
            ) {
                MiniPlayerMorphState.miniArtBounds
            } else {
                null
            }
        }

    val progress =
        remember {
            Animatable(
                if (
                    start == null
                ) {
                    1f
                } else {
                    0f
                }
            )
        }

    var ownBounds by remember {
        mutableStateOf<Rect?>(
            null
        )
    }

    val ownBoundsKnown =
        ownBounds != null

    LaunchedEffect(
        ownBoundsKnown
    ) {
        if (
            start != null &&
            ownBoundsKnown
        ) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 420f
                )
            )

            MiniPlayerMorphState.armed =
                false
        }
    }

    DisposableEffect(
        Unit
    ) {
        onDispose {
            MiniPlayerMorphState.armed =
                false
        }
    }

    return this
        .onGloballyPositioned { coordinates ->
            if (
                ownBounds == null
            ) {
                ownBounds =
                    coordinates.boundsInRoot()
            }
        }
        .graphicsLayer {
            val from =
                start

            val to =
                ownBounds

            if (
                from == null
            ) {
                return@graphicsLayer
            }

            /*
             * Hide the art for the one frame before its real position
             * is measured so it doesn't flash at full size.
             */
            if (
                to == null
            ) {
                alpha =
                    0f

                return@graphicsLayer
            }

            val amount =
                progress.value

            if (
                amount >= 1f
            ) {
                return@graphicsLayer
            }

            transformOrigin =
                TransformOrigin(
                    0f,
                    0f
                )

            val scale =
                lerp(
                    from.width / to.width,
                    1f,
                    amount
                )

            scaleX =
                scale

            scaleY =
                scale

            translationX =
                (from.left - to.left) *
                        (1f - amount)

            translationY =
                (from.top - to.top) *
                        (1f - amount)
        }
}