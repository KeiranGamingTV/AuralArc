package com.keiranhaas.auralarc.ui.driving

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.player.AuralArcRepeatMode
import com.keiranhaas.auralarc.player.PlaybackService
import com.keiranhaas.auralarc.player.PlaybackState
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.storage.LikedSongsStore
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import com.keiranhaas.auralarc.utils.ArtworkBitmapLoader
import com.keiranhaas.auralarc.utils.formatTime

private val DrivingControlColor =
    Color(0xFF292929)

@OptIn(UnstableApi::class)
@Composable
fun DrivingModeScreen(
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val activity = context as? Activity

    val queueRevision = QueueManager.revision.value
    val currentTrack = remember(queueRevision) {
        QueueManager.currentTrack()
    }
    val isPlaying = PlayerManager.isPlaying.value

    var artwork by remember {
        mutableStateOf<Bitmap?>(null)
    }

    DisposableEffect(activity) {
        activity?.window?.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        onDispose {
            activity?.window?.clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    LaunchedEffect(currentTrack?.albumArtPath) {
        val path = currentTrack?.albumArtPath

        artwork = if (path.isNullOrBlank()) {
            null
        } else {
            ArtworkBitmapLoader.load(
                context,
                path,
                with(density) {
                    280.dp.roundToPx()
                }
            )
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.screenWidthDp > configuration.screenHeightDp

    Scaffold(
        containerColor = AuralArcStyle.Surface
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(AuralArcStyle.Surface)
                .pointerInput(Unit) {
                    var totalDrag = 0f

                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            totalDrag += amount
                        },
                        onDragEnd = {
                            when {
                                totalDrag < -70f -> PlayerManager.next(context)
                                totalDrag > 70f -> PlayerManager.previous(context)
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = {
                            totalDrag = 0f
                        }
                    )
                }
                .padding(
                    horizontal = if (isLandscape) 24.dp else 20.dp,
                    vertical = if (isLandscape) 10.dp else 10.dp
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DrivingArtwork(
                            artwork = artwork,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DrivingTrackInfo(
                            track = currentTrack,
                            titleSize = 24.sp,
                            artistSize = 18.sp,
                            showAlbum = false
                        )

                        DrivingProgress()

                        DrivingTransportRow(
                            isPlaying = isPlaying,
                            sideSize = 72.dp,
                            centerSize = 88.dp
                        )

                        DrivingExtrasRow(
                            track = currentTrack,
                            buttonSize = 48.dp
                        )

                        DrivingExitButton(
                            onExit = onExit,
                            height = 48.dp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    DrivingArtwork(
                        artwork = artwork,
                        modifier = Modifier.size(220.dp)
                    )

                    DrivingTrackInfo(
                        track = currentTrack,
                        titleSize = 27.sp,
                        artistSize = 20.sp,
                        showAlbum = true
                    )

                    DrivingProgress()

                    DrivingTransportRow(
                        isPlaying = isPlaying,
                        sideSize = 88.dp,
                        centerSize = 108.dp
                    )

                    DrivingExtrasRow(
                        track = currentTrack,
                        buttonSize = 56.dp
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Swipe left for next · right for previous",
                            color = AuralArcStyle.TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        DrivingExitButton(
                            onExit = onExit,
                            height = 56.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivingArtwork(
    artwork: Bitmap?,
    modifier: Modifier = Modifier
) {
    if (artwork != null) {
        Image(
            bitmap = artwork.asImageBitmap(),
            contentDescription = "Album artwork",
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(DrivingControlColor)
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(22.dp))
                .background(DrivingControlColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AuralArc",
                color = AuralArcStyle.TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DrivingTrackInfo(
    track: MusicTrack?,
    titleSize: androidx.compose.ui.unit.TextUnit,
    artistSize: androidx.compose.ui.unit.TextUnit,
    showAlbum: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = track?.title ?: "Nothing playing",
            color = AuralArcStyle.TextPrimary,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = track?.artist ?: "Choose a song in your library",
            color = AuralArcStyle.TextSecondary,
            fontSize = artistSize,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (showAlbum) {
            track?.album
                ?.takeIf { it.isNotBlank() }
                ?.let { album ->
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = album,
                        color = AuralArcStyle.TextSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
        }
    }
}

/**
 * Read-only progress bar. It is deliberately not draggable so a
 * bump while driving can never skip around inside a song.
 */
@Composable
private fun DrivingProgress() {
    val position = PlayerManager.currentPosition.value
    val duration = PlayerManager.duration.value

    val fraction =
        if (duration > 0L) {
            (position.toFloat() / duration.toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DrivingControlColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(AuralArcStyle.PurpleBright)
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                color = AuralArcStyle.TextSecondary,
                fontSize = 14.sp
            )

            Text(
                text = formatTime(duration),
                color = AuralArcStyle.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DrivingTransportRow(
    isPlaying: Boolean,
    sideSize: Dp,
    centerSize: Dp
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrivingControlButton(
            onClick = { PlayerManager.previous(context) },
            modifier = Modifier.size(sideSize),
            background = DrivingControlColor
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous track",
                tint = AuralArcStyle.TextPrimary,
                modifier = Modifier.size(sideSize * 0.5f)
            )
        }

        DrivingControlButton(
            onClick = {
                if (isPlaying) PlayerManager.pause()
                else PlayerManager.resume()
            },
            modifier = Modifier.size(centerSize),
            background = AuralArcStyle.TextPrimary
        ) {
            Icon(
                imageVector = if (isPlaying) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(centerSize * 0.5f)
            )
        }

        DrivingControlButton(
            onClick = { PlayerManager.next(context) },
            modifier = Modifier.size(sideSize),
            background = DrivingControlColor
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next track",
                tint = AuralArcStyle.TextPrimary,
                modifier = Modifier.size(sideSize * 0.5f)
            )
        }
    }
}

@Composable
private fun DrivingExtrasRow(
    track: MusicTrack?,
    buttonSize: Dp
) {
    val context = LocalContext.current

    val shuffleOn =
        PlaybackState.shuffleEnabled.value

    val repeatMode =
        PlaybackState.repeatMode.value

    val likedRevision =
        LikedSongsStore.revision.value

    val isLiked =
        remember(track?.uri, likedRevision) {
            track != null &&
                    LikedSongsStore.isLiked(
                        context,
                        track
                    )
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrivingControlButton(
            onClick = {
                PlaybackState.toggleShuffle()

                PlayerManager.applyPlaybackModes()

                PlaybackService.requestControlRefresh(
                    context
                )
            },
            modifier = Modifier.size(buttonSize),
            background = DrivingControlColor
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleOn) {
                    AuralArcStyle.PurpleBright
                } else {
                    AuralArcStyle.TextMuted
                },
                modifier = Modifier.size(buttonSize * 0.5f)
            )
        }

        DrivingControlButton(
            onClick = {
                if (track != null) {
                    LikedSongsStore.toggle(
                        context,
                        track
                    )
                }
            },
            modifier = Modifier.size(buttonSize),
            background = DrivingControlColor
        ) {
            Icon(
                imageVector = if (isLiked) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                contentDescription = if (isLiked) {
                    "Remove from Liked Songs"
                } else {
                    "Add to Liked Songs"
                },
                tint = if (isLiked) {
                    AuralArcStyle.PurpleBright
                } else {
                    AuralArcStyle.TextMuted
                },
                modifier = Modifier.size(buttonSize * 0.5f)
            )
        }

        DrivingControlButton(
            onClick = {
                PlaybackState.cycleRepeatMode()

                PlayerManager.applyRepeatOnly()

                PlaybackService.requestControlRefresh(
                    context
                )
            },
            modifier = Modifier.size(buttonSize),
            background = DrivingControlColor
        ) {
            Icon(
                imageVector = if (repeatMode == AuralArcRepeatMode.ONE) {
                    Icons.Filled.RepeatOne
                } else {
                    Icons.Filled.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode == AuralArcRepeatMode.OFF) {
                    AuralArcStyle.TextMuted
                } else {
                    AuralArcStyle.PurpleBright
                },
                modifier = Modifier.size(buttonSize * 0.5f)
            )
        }
    }
}

@Composable
private fun DrivingExitButton(
    onExit: () -> Unit,
    height: Dp
) {
    FilledTonalButton(
        onClick = onExit,
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(height),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = DrivingControlColor,
            contentColor = AuralArcStyle.TextPrimary
        )
    ) {
        Text(
            text = "Exit Driving Mode",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DrivingControlButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = DrivingControlColor,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}