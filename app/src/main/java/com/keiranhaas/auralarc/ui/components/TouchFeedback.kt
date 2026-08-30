package com.keiranhaas.auralarc.ui

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.keiranhaas.auralarc.ui.theme.AuralArcMotion
import com.keiranhaas.auralarc.ui.theme.rememberAuralArcMotionEnabled

private fun performNativeTouchFeedback(
    view: View
) {
    view.playSoundEffect(
        SoundEffectConstants.CLICK
    )

    view.performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY
    )
}

@Composable
fun rememberAuralArcClickFeedback(): () -> Unit {
    val view =
        LocalView.current

    return remember(
        view
    ) {
        {
            performNativeTouchFeedback(
                view
            )
        }
    }
}

fun Modifier.auralArcClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val view =
        LocalView.current

    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val isPressed by
    interactionSource.collectIsPressedAsState()

    val scale by
    animateFloatAsState(
        targetValue =
            if (
                motionEnabled &&
                enabled &&
                isPressed
            ) {
                AuralArcMotion.CARD_PRESS_SCALE
            } else {
                1f
            },
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 700f
        )
    )

    this
        .graphicsLayer {
            scaleX =
                scale

            scaleY =
                scale
        }
        .clickable(
            interactionSource =
                interactionSource,
            indication =
                LocalIndication.current,
            enabled = enabled,
            onClick = {
                performNativeTouchFeedback(
                    view
                )

                onClick()
            }
        )
}

@Composable
fun AuralArcIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val clickFeedback =
        rememberAuralArcClickFeedback()

    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val isPressed by
    interactionSource.collectIsPressedAsState()

    val scale by
    animateFloatAsState(
        targetValue =
            if (
                motionEnabled &&
                enabled &&
                isPressed
            ) {
                AuralArcMotion.BUTTON_PRESS_SCALE
            } else {
                1f
            },
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 750f
        )
    )

    IconButton(
        onClick = {
            clickFeedback()

            onClick()
        },
        modifier = modifier
            .graphicsLayer {
                scaleX =
                    scale

                scaleY =
                    scale
            },
        enabled = enabled,
        interactionSource =
            interactionSource,
        content = content
    )
}

@Composable
fun AuralArcButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val clickFeedback =
        rememberAuralArcClickFeedback()

    val motionEnabled =
        rememberAuralArcMotionEnabled()

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val isPressed by
    interactionSource.collectIsPressedAsState()

    val scale by
    animateFloatAsState(
        targetValue =
            if (
                motionEnabled &&
                enabled &&
                isPressed
            ) {
                AuralArcMotion.BUTTON_PRESS_SCALE
            } else {
                1f
            },
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = 750f
        )
    )

    Button(
        onClick = {
            clickFeedback()

            onClick()
        },
        modifier = modifier
            .graphicsLayer {
                scaleX =
                    scale

                scaleY =
                    scale
            },
        enabled = enabled,
        interactionSource =
            interactionSource,
        content = content
    )
}