package com.keiranhaas.auralarc.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import com.keiranhaas.auralarc.utils.ArtworkBitmapLoader
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcMotion

@Composable
fun TrackArtwork(
    albumArtPath: String?,
    size: Dp
) {
    val context =
        LocalContext.current

    val density =
        LocalDensity.current

    val artworkKey =
        albumArtPath
            ?.trim()
            .orEmpty()

    val targetSizePx =
        with(
            density
        ) {
            size.roundToPx()
        }.coerceAtLeast(
            1
        )

    var bitmap by remember(
        artworkKey,
        targetSizePx
    ) {
        mutableStateOf<Bitmap?>(
            ArtworkBitmapLoader.peek(
                artworkPath =
                    artworkKey,
                targetSizePx =
                    targetSizePx
            )
        )
    }

    LaunchedEffect(
        artworkKey,
        targetSizePx
    ) {
        if (
            artworkKey.isBlank()
        ) {
            bitmap =
                null

            return@LaunchedEffect
        }

        bitmap =
            ArtworkBitmapLoader.load(
                context = context,
                artworkPath =
                    artworkKey,
                targetSizePx =
                    targetSizePx
            )
    }

    AuralArcCard(
        modifier = Modifier.size(
            size
        ),
        shape = AuralArcStyle.CardShape,
        backgroundColor =
            AuralArcStyle.Surface,
        elevation = 4.dp
    ) {
        val loadedBitmap =
            bitmap

        Crossfade(
            targetState = loadedBitmap,
            animationSpec =
                tween(
                    durationMillis =
                        AuralArcMotion.FAST,
                    easing =
                        FastOutSlowInEasing
                )
        ) { artwork ->
            if (
                artwork != null
            ) {
                Image(
                    bitmap =
                        artwork.asImageBitmap(),
                    contentDescription =
                        "Album artwork",
                    modifier =
                        Modifier.fillMaxSize(),
                    contentScale =
                        ContentScale.Crop
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                AuralArcStyle.SurfaceBright
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text = "♪",
                        style =
                            MaterialTheme.typography.h4,
                        color =
                            AuralArcStyle.TextMuted
                    )
                }
            }
        }
    }
}