package com.example.auralarc.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

object AuralArcMotion {

    const val FAST =
        90

    const val NORMAL =
        150

    const val SLOW =
        220
}

@Composable
fun <T> AuralArcCrossfade(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        animationSpec = tween(
            durationMillis = AuralArcMotion.NORMAL,
            easing = FastOutSlowInEasing
        )
    ) { state ->
        content(
            state
        )
    }
}