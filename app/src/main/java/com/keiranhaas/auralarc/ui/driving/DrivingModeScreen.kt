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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import com.keiranhaas.auralarc.utils.ArtworkBitmapLoader

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

    val backgroundColor = AuralArcStyle.Surface
    val primaryTextColor = AuralArcStyle.TextPrimary
    val secondaryTextColor = AuralArcStyle.TextSecondary

    val controlBackground = Color(0xFF292929)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Scaffold(
        backgroundColor = backgroundColor
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .background(backgroundColor)
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
                    vertical = if (isLandscape) 12.dp else 10.dp
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork occupies the left side.
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artwork != null) {
                            Image(
                                bitmap = artwork!!.asImageBitmap(),
                                contentDescription = "Album artwork",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .background(
                                        controlBackground,
                                        RoundedCornerShape(22.dp)
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .background(
                                        controlBackground,
                                        RoundedCornerShape(22.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AuralArc",
                                    color = primaryTextColor,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Track information and controls occupy the right side.
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentTrack?.title ?: "Nothing playing",
                                color = primaryTextColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = currentTrack?.artist
                                    ?: "Choose a song in your library",
                                color = secondaryTextColor,
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            currentTrack?.album
                                ?.takeIf { it.isNotBlank() }
                                ?.let { album ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = album,
                                        color = secondaryTextColor,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DrivingControlButton(
                                description = "Previous track",
                                onClick = { PlayerManager.previous(context) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = null,
                                        tint = primaryTextColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier.size(76.dp),
                                background = controlBackground
                            )

                            DrivingControlButton(
                                description = if (isPlaying) "Pause" else "Play",
                                onClick = {
                                    if (isPlaying) PlayerManager.pause()
                                    else PlayerManager.resume()
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isPlaying) {
                                            Icons.Filled.Pause
                                        } else {
                                            Icons.Filled.PlayArrow
                                        },
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(48.dp)
                                    )
                                },
                                modifier = Modifier.size(96.dp),
                                background = primaryTextColor
                            )

                            DrivingControlButton(
                                description = "Next track",
                                onClick = { PlayerManager.next(context) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = null,
                                        tint = primaryTextColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                },
                                modifier = Modifier.size(76.dp),
                                background = controlBackground
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Swipe left for next · right for previous",
                                color = secondaryTextColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            androidx.compose.material.OutlinedButton(
                                onClick = onExit
                            ) {
                                Text(
                                    text = "Exit Driving Mode",
                                    color = primaryTextColor
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait retains the familiar vertically stacked layout.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (artwork != null) {
                        Image(
                            bitmap = artwork!!.asImageBitmap(),
                            contentDescription = "Album artwork",
                            modifier = Modifier
                                .size(220.dp)
                                .background(
                                    controlBackground,
                                    RoundedCornerShape(22.dp)
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(
                                    controlBackground,
                                    RoundedCornerShape(22.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AuralArc",
                                color = primaryTextColor,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentTrack?.title ?: "Nothing playing",
                            color = primaryTextColor,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = currentTrack?.artist
                                ?: "Choose a song in your library",
                            color = secondaryTextColor,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        currentTrack?.album
                            ?.takeIf { it.isNotBlank() }
                            ?.let { album ->
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = album,
                                    color = secondaryTextColor,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrivingControlButton(
                            description = "Previous track",
                            onClick = { PlayerManager.previous(context) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = null,
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            },
                            modifier = Modifier.size(88.dp),
                            background = controlBackground
                        )

                        DrivingControlButton(
                            description = if (isPlaying) "Pause" else "Play",
                            onClick = {
                                if (isPlaying) PlayerManager.pause()
                                else PlayerManager.resume()
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isPlaying) {
                                        Icons.Filled.Pause
                                    } else {
                                        Icons.Filled.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(52.dp)
                                )
                            },
                            modifier = Modifier.size(108.dp),
                            background = primaryTextColor
                        )

                        DrivingControlButton(
                            description = "Next track",
                            onClick = { PlayerManager.next(context) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = null,
                                    tint = primaryTextColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            },
                            modifier = Modifier.size(88.dp),
                            background = controlBackground
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Swipe left for next · right for previous",
                            color = secondaryTextColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        androidx.compose.material.OutlinedButton(
                            onClick = onExit
                        ) {
                            Text(
                                text = "Exit Driving Mode",
                                color = primaryTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrivingControlButton(
    description: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF292929)
) {
    androidx.compose.material.Surface(
        modifier = modifier.clickable(
            onClick = onClick
        ),
        color = background,
        shape = RoundedCornerShape(50),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}