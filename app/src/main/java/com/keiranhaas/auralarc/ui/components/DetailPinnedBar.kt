package com.keiranhaas.auralarc.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun rememberDetailPinnedAlpha(
    listState: LazyListState
): State<Float> {
    val density =
        LocalDensity.current

    val startPx =
        with(
            density
        ) {
            150.dp.toPx()
        }

    val rangePx =
        with(
            density
        ) {
            60.dp.toPx()
        }

    return remember(
        listState,
        startPx,
        rangePx
    ) {
        derivedStateOf {
            if (
                listState.firstVisibleItemIndex > 0
            ) {
                1f
            } else {
                (
                        (
                                listState.firstVisibleItemScrollOffset - startPx
                                ) / rangePx
                        ).coerceIn(
                        0f,
                        1f
                    )
            }
        }
    }
}

@Composable
fun DetailPinnedBar(
    alpha: Float,
    content: @Composable () -> Unit
) {
    if (
        alpha > 0.01f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha =
                        alpha

                    translationY =
                        (alpha - 1f) * 16.dp.toPx()
                }
        ) {
            content()
        }
    }
}