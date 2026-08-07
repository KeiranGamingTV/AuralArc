package com.example.auralarc.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.TransformOrigin

@OptIn(
    ExperimentalTextApi::class
)
@Composable
fun OscillatingMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Start,
    startDelayMillis: Long = 900L,
    endPauseMillis: Long = 700L,
    pixelsPerSecond: Float = 28f
) {
    val density =
        LocalDensity.current

    val textMeasurer =
        rememberTextMeasurer()

    val resolvedStyle =
        remember(
            style,
            color,
            fontWeight
        ) {
            style.merge(
                TextStyle(
                    color = color,
                    fontWeight = fontWeight
                )
            )
        }

    val measuredTextWidthPx =
        remember(
            text,
            resolvedStyle
        ) {
            textMeasurer.measure(
                text =
                AnnotatedString(
                    text
                ),
                style = resolvedStyle,
                softWrap = false,
                maxLines = 1,
                constraints =
                Constraints()
            ).size.width
        }

    /*
     * Leave a small amount of invisible space on both sides
     * of the moving text.
     *
     * TextMeasurer reports the layout width, but individual
     * glyphs can visually extend slightly outside those bounds.
     * Without this padding, clipToBounds() can shave off the
     * beginning of the first character when the marquee is at
     * its starting position.
     */
    val marqueeEdgePadding =
        6.dp

    val marqueeEdgePaddingPx =
        with(
            density
        ) {
            marqueeEdgePadding.roundToPx()
        }

    val renderedTextWidthPx =
        (
                measuredTextWidthPx +
                        marqueeEdgePaddingPx * 2
                ).coerceAtLeast(
                1
            )

    var availableWidthPx by remember {
        mutableStateOf(
            0
        )
    }

    val maximumOffsetPx =
        (
                renderedTextWidthPx -
                        availableWidthPx
                )
            .coerceAtLeast(
                0
            )
            .toFloat()

    val animatedOffset =
        remember(
            text
        ) {
            Animatable(
                0f
            )
        }

    LaunchedEffect(
        text,
        maximumOffsetPx
    ) {
        animatedOffset.snapTo(
            0f
        )

        if (
            maximumOffsetPx <= 0f
        ) {
            return@LaunchedEffect
        }

        val animationDuration =
            (
                    maximumOffsetPx /
                            pixelsPerSecond *
                            1_000f
                    )
                .roundToInt()
                .coerceIn(
                    1_400,
                    18_000
                )

        while (
            true
        ) {
            delay(
                startDelayMillis
            )

            animatedOffset.animateTo(
                targetValue =
                maximumOffsetPx,
                animationSpec =
                tween(
                    durationMillis =
                    animationDuration,
                    easing =
                    LinearEasing
                )
            )

            delay(
                endPauseMillis
            )

            animatedOffset.animateTo(
                targetValue =
                0f,
                animationSpec =
                tween(
                    durationMillis =
                    animationDuration,
                    easing =
                    LinearEasing
                )
            )

            delay(
                endPauseMillis
            )
        }
    }

    val fullTextWidth =
        with(
            density
        ) {
            renderedTextWidthPx.toDp()
        }

    val idleAlignment =
        when (
            textAlign
        ) {
            TextAlign.Center ->
                Alignment.Center

            TextAlign.End ->
                Alignment.CenterEnd

            else ->
                Alignment.CenterStart
        }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                availableWidthPx =
                    size.width
            }
            .clipToBounds(),
        contentAlignment =
        if (
            maximumOffsetPx > 0f
        ) {
            Alignment.CenterStart
        } else {
            idleAlignment
        }
    ) {
        if (
            maximumOffsetPx > 0f
        ) {
            Box(
                modifier = Modifier
                    /*
                     * Measure the marquee child without the parent's
                     * horizontal width restriction, but explicitly
                     * anchor the oversized result to the START.
                     *
                     * Without this, requiredWidth() can cause Compose
                     * to center the overflowing child inside the
                     * constrained title area. That makes the first
                     * part of the title already sit offscreen even
                     * when animatedOffset is 0.
                     */
                    .wrapContentWidth(
                        align = Alignment.Start,
                        unbounded = true
                    )
                    .requiredWidth(
                        fullTextWidth
                    )
                    .graphicsLayer {
                        transformOrigin =
                            TransformOrigin(
                                pivotFractionX = 0f,
                                pivotFractionY = 0.5f
                            )

                        translationX =
                            -animatedOffset.value
                    },
                contentAlignment =
                    Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    style = resolvedStyle,
                    maxLines = 1,
                    softWrap = false,
                    overflow =
                    TextOverflow.Visible,
                    textAlign =
                    TextAlign.Start,
                    modifier = Modifier.padding(
                        horizontal =
                            marqueeEdgePadding
                    )
                )
            }
        } else {
            Text(
                text = text,
                style = resolvedStyle,
                maxLines = 1,
                softWrap = false,
                overflow =
                TextOverflow.Clip,
                textAlign = textAlign,
                modifier =
                Modifier.fillMaxWidth()
            )
        }
    }
}