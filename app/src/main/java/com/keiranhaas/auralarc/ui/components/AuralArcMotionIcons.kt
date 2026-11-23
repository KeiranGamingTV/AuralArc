package com.keiranhaas.auralarc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcMotion
import com.keiranhaas.auralarc.ui.theme.rememberAuralArcMotionEnabled
import kotlinx.coroutines.delay

private val PlayLeft =
    floatArrayOf(8f, 5f, 13.5f, 8.5f, 13.5f, 15.5f, 8f, 19f)

private val PauseLeft =
    floatArrayOf(6f, 5f, 10f, 5f, 10f, 19f, 6f, 19f)

private val PlayRight =
    floatArrayOf(13.5f, 8.5f, 19f, 12f, 19f, 12f, 13.5f, 15.5f)

private val PauseRight =
    floatArrayOf(14f, 5f, 18f, 5f, 18f, 19f, 14f, 19f)

@Composable
fun PlayPauseIcon(
    isPlaying: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null
) {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val progress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec =
            if (motionEnabled) {
                tween<Float>(
                    durationMillis = AuralArcMotion.NORMAL
                )
            } else {
                snap<Float>()
            },
        label = "playPauseMorph"
    )

    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
            }
        } else {
            Modifier
        }

    Canvas(
        modifier = modifier
            .size(iconSize)
            .then(semanticsModifier)
    ) {
        val unit = this.size.minDimension / 24f

        fun piece(from: FloatArray, to: FloatArray) {
            val path = Path()

            for (i in 0 until 4) {
                val x =
                    (from[i * 2] + (to[i * 2] - from[i * 2]) * progress) * unit
                val y =
                    (from[i * 2 + 1] + (to[i * 2 + 1] - from[i * 2 + 1]) * progress) * unit

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            path.close()

            drawPath(path = path, color = tint)

            drawPath(
                path = path,
                color = tint,
                style = Stroke(
                    width = unit * 1.2f,
                    join = StrokeJoin.Round
                )
            )
        }

        piece(PlayLeft, PauseLeft)
        piece(PlayRight, PauseRight)
    }
}

@Composable
fun PopIcon(
    imageVector: ImageVector,
    tint: Color,
    contentDescription: String?,
    popKey: Any,
    modifier: Modifier = Modifier,
    popEnabled: Boolean = true
) {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val animatedTint by animateColorAsState(
        targetValue = tint,
        animationSpec =
            if (motionEnabled) {
                tween<Color>(
                    durationMillis = AuralArcMotion.NORMAL
                )
            } else {
                snap<Color>()
            },
        label = "popIconTint"
    )

    val scale = remember { Animatable(1f) }

    var seenFirstKey by remember { mutableStateOf(false) }

    LaunchedEffect(popKey) {
        if (!seenFirstKey) {
            seenFirstKey = true
            return@LaunchedEffect
        }

        if (motionEnabled && popEnabled) {
            scale.snapTo(0.7f)

            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.4f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = animatedTint,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    )
}

fun Modifier.menuReveal(
    index: Int
): Modifier = composed {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val progress = remember {
        Animatable(
            if (motionEnabled) 0f else 1f
        )
    }

    LaunchedEffect(Unit) {
        if (motionEnabled) {
            delay(index * 28L)

            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = AuralArcMotion.NAV_DAMPING_RATIO,
                    stiffness = AuralArcMotion.NAV_STIFFNESS
                )
            )
        }
    }

    graphicsLayer {
        val p = progress.value.coerceIn(0f, 1f)

        alpha = p
        translationY = (1f - p) * -8.dp.toPx()
    }
}