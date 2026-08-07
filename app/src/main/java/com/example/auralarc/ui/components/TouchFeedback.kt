package com.example.auralarc.ui

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Button
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

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

    this.clickable(
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

    IconButton(
        onClick = {
            clickFeedback()

            onClick()
        },
        modifier = modifier,
        enabled = enabled,
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

    Button(
        onClick = {
            clickFeedback()

            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}