package com.example.auralarc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.navigation.Screen
import com.example.auralarc.player.AuralArcRepeatMode
import com.example.auralarc.player.PlaybackState
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.player.QueueManager
import com.example.auralarc.ui.theme.AuralArcStyle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import com.example.auralarc.utils.audioQualitySummary
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import com.example.auralarc.storage.DuetLrcParser
import com.example.auralarc.storage.DuetLyricRow
import com.example.auralarc.storage.EmbeddedLyricsType
import androidx.compose.ui.unit.sp
import com.example.auralarc.player.PlaybackService

private val DuetSingerOneColor =
    Color(
        0xFF00E5FF
    )

private val DuetSingerTwoColor =
    Color(
        0xFFFF5252
    )

@Composable
fun NowPlayingScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val currentTrack =
        QueueManager.currentTrack()

    val lyricsTrack =
        currentTrack

    val queue =
        QueueManager.getQueue()

    var showLyricsCard by remember {
        mutableStateOf(
            false
        )
    }

    var sliderPosition by remember {
        mutableStateOf(
            0f
        )
    }

    var userIsSeeking by remember {
        mutableStateOf(
            false
        )
    }

    val duration =
        PlayerManager.duration.value.coerceAtLeast(
            0L
        )

    val currentPosition =
        PlayerManager.currentPosition.value.coerceAtLeast(
            0L
        )

    fun closeNowPlayingOrLyrics() {
        if (
            showLyricsCard
        ) {
            showLyricsCard =
                false
        } else {
            navController.popBackStack()
        }
    }

    BackHandler {
        closeNowPlayingOrLyrics()
    }

    LaunchedEffect(
        showLyricsCard,
        currentTrack?.uri
    ) {
        if (
            showLyricsCard &&
            currentTrack != null
        ) {
            LyricsState.preloadLyrics(
                context = context.applicationContext,
                track = currentTrack
            )
        }
    }

    LaunchedEffect(
        currentTrack?.uri
    ) {
        userIsSeeking =
            false

        sliderPosition =
            0f
    }

    LaunchedEffect(
        currentPosition,
        userIsSeeking
    ) {
        if (
            !userIsSeeking
        ) {
            sliderPosition =
                currentPosition.toFloat()
        }
    }

    Scaffold(
        backgroundColor = AuralArcStyle.BackgroundBottom,
        topBar = {
            TopAppBar(
                backgroundColor = AuralArcStyle.BackgroundTop,
                elevation = 0.dp,
                navigationIcon = {
                    AuralArcIconButton(
                        onClick = {
                            closeNowPlayingOrLyrics()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text =
                        if (
                            showLyricsCard
                        ) {
                            "Lyrics"
                        } else {
                            "Now Playing"
                        },
                        color = AuralArcStyle.TextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .background(
                    brush = AuralArcStyle.appBackgroundBrush()
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (
                    showLyricsCard &&
                    lyricsTrack != null
                ) {
                    NowPlayingLyricsCard(
                        track = lyricsTrack
                    )
                } else if (
                    PlayerManager.currentTitle.value.isBlank()
                ) {
                    AuralArcMessageCard(
                        title = "Nothing Playing",
                        message = "Choose a song from your library to start playback."
                    )
                } else {
                    NowPlayingMainContent(
                        navController = navController,
                        context = context,
                        currentTrack = currentTrack,
                        queue = queue,
                        duration = duration,
                        sliderPosition = sliderPosition,
                        onSliderPositionChange = { newValue ->
                            sliderPosition =
                                newValue
                        },
                        userIsSeeking = userIsSeeking,
                        onUserIsSeekingChange = { seeking ->
                            userIsSeeking =
                                seeking
                        },
                        onShowLyrics = {
                            showLyricsCard =
                                true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingMainContent(
    navController: NavHostController,
    context: android.content.Context,
    currentTrack: MusicTrack?,
    queue: List<MusicTrack>,
    duration: Long,
    sliderPosition: Float,
    onSliderPositionChange: (Float) -> Unit,
    userIsSeeking: Boolean,
    onUserIsSeekingChange: (Boolean) -> Unit,
    onShowLyrics: (MusicTrack) -> Unit
) {
    var latestDraggedPosition by remember(
        currentTrack?.uri
    ) {
        mutableStateOf(
            sliderPosition
        )
    }

    LaunchedEffect(
        sliderPosition,
        userIsSeeking
    ) {
        if (
            !userIsSeeking
        ) {
            latestDraggedPosition =
                sliderPosition
        }
    }

    val screenHeightDp =
        LocalConfiguration.current.screenHeightDp

    val artworkSize =
        when {
            screenHeightDp < 700 ->
                190.dp

            screenHeightDp < 820 ->
                225.dp

            else ->
                260.dp
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                bottom = 28.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = AuralArcStyle.CardShape,
            backgroundColor = AuralArcStyle.SurfaceBright,
            elevation = 14.dp,
            modifier = Modifier.padding(
                top = 4.dp
            )
        ) {
            TrackArtwork(
                albumArtPath =
                PlayerManager.currentAlbumArtPath.value,
                size = artworkSize
            )
        }

        Spacer(
            modifier = Modifier.height(
                16.dp
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (
                currentTrack != null
            ) {
                TrackTitleWithHdBadge(
                    track = currentTrack,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.h5,
                    color = AuralArcStyle.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    horizontalArrangement =
                    Arrangement.Center,
                    fillTitleWeight = true,
                    marqueeWhenOverflow = true,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = PlayerManager.currentTitle.value,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(
                modifier = Modifier.height(
                    7.dp
                )
            )

            Text(
                text =
                PlayerManager.currentArtist.value.ifBlank {
                    "Unknown Artist"
                },
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(
            modifier = Modifier.height(
                14.dp
            )
        )

        if (
            duration <= 0L
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Slider(
                value =
                if (
                    userIsSeeking
                ) {
                    latestDraggedPosition.coerceIn(
                        0f,
                        duration.toFloat()
                    )
                } else {
                    sliderPosition.coerceIn(
                        0f,
                        duration.toFloat()
                    )
                },
                onValueChange = { newPosition ->
                    latestDraggedPosition =
                        newPosition

                    onUserIsSeekingChange(
                        true
                    )

                    onSliderPositionChange(
                        newPosition
                    )
                },
                onValueChangeFinished = {
                    val requestedPosition =
                        latestDraggedPosition
                            .toLong()
                            .coerceIn(
                                0L,
                                duration
                            )

                    onSliderPositionChange(
                        requestedPosition.toFloat()
                    )

                    PlayerManager.seekTo(
                        requestedPosition
                    )

                    onUserIsSeekingChange(
                        false
                    )
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
            Arrangement.SpaceBetween
        ) {
            Text(
                text =
                if (
                    duration <= 0L
                ) {
                    "0:00"
                } else {
                    formatNowPlayingTime(
                        sliderPosition.toLong()
                    )
                },
                style = MaterialTheme.typography.caption,
                color = AuralArcStyle.TextMuted
            )

            Text(
                text =
                if (
                    duration <= 0L
                ) {
                    "--:--"
                } else {
                    formatNowPlayingTime(
                        duration
                    )
                },
                style = MaterialTheme.typography.caption,
                color = AuralArcStyle.TextMuted
            )
        }

        Text(
            text = audioQualitySummary(
                currentTrack
            ),
            style = MaterialTheme.typography.caption,
            color = AuralArcStyle.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 5.dp
                )
        )

        Spacer(
            modifier = Modifier.height(
                14.dp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
            Arrangement.SpaceEvenly,
            verticalAlignment =
            Alignment.CenterVertically
        ) {
            AuralArcIconButton(
                onClick = {
                    PlaybackState.toggleShuffle()

                    PlayerManager.applyPlaybackModes()

                    PlaybackService.requestControlRefresh(
                        context
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint =
                    if (
                        PlaybackState.shuffleEnabled.value
                    ) {
                        AuralArcStyle.PurpleBright
                    } else {
                        AuralArcStyle.TextMuted
                    }
                )
            }

            AuralArcIconButton(
                onClick = {
                    PlayerManager.previous(
                        context
                    )
                }
            ) {
                Icon(
                    imageVector =
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = AuralArcStyle.TextPrimary,
                    modifier = Modifier.size(
                        34.dp
                    )
                )
            }

            Card(
                shape = AuralArcStyle.CardShape,
                backgroundColor = AuralArcStyle.Purple,
                elevation = 12.dp
            ) {
                AuralArcIconButton(
                    onClick = {
                        if (
                            PlayerManager.isPlaying.value
                        ) {
                            PlayerManager.pause()
                        } else {
                            PlayerManager.resume()
                        }
                    },
                    modifier = Modifier.size(
                        72.dp
                    )
                ) {
                    Icon(
                        imageVector =
                        if (
                            PlayerManager.isPlaying.value
                        ) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription =
                        if (
                            PlayerManager.isPlaying.value
                        ) {
                            "Pause"
                        } else {
                            "Play"
                        },
                        tint = AuralArcStyle.TextPrimary,
                        modifier = Modifier.size(
                            42.dp
                        )
                    )
                }
            }

            AuralArcIconButton(
                onClick = {
                    PlayerManager.next(
                        context
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = AuralArcStyle.TextPrimary,
                    modifier = Modifier.size(
                        34.dp
                    )
                )
            }

            AuralArcIconButton(
                onClick = {
                    PlaybackState.cycleRepeatMode()

                    PlayerManager.applyRepeatOnly()

                    PlaybackService.requestControlRefresh(
                        context
                    )
                }
            ) {
                Icon(
                    imageVector =
                    if (
                        PlaybackState.repeatMode.value ==
                        AuralArcRepeatMode.ONE
                    ) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint =
                    if (
                        PlaybackState.repeatMode.value ==
                        AuralArcRepeatMode.OFF
                    ) {
                        AuralArcStyle.TextMuted
                    } else {
                        AuralArcStyle.PurpleBright
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                10.dp
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 44.dp
                ),
            horizontalArrangement =
            Arrangement.SpaceEvenly,
            verticalAlignment =
            Alignment.Top
        ) {
            Column(
                horizontalAlignment =
                Alignment.CenterHorizontally
            ) {
                AuralArcIconButton(
                    onClick = {
                        navController.navigate(
                            Screen.Queue.route
                        )
                    }
                ) {
                    Icon(
                        imageVector =
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Open queue",
                        tint = AuralArcStyle.TextPrimary
                    )
                }

                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )
            }

            if (
                currentTrack != null
            ) {
                Column(
                    horizontalAlignment =
                    Alignment.CenterHorizontally
                ) {
                    MoreOptionsButton(
                        track = currentTrack,
                        queueTracks =
                        if (
                            queue.isNotEmpty()
                        ) {
                            queue
                        } else {
                            listOf(
                                currentTrack
                            )
                        },
                        showAddAlbumToPlaylist = false,
                        showAddToQueue = false,
                        showPlayNext = false,
                        onOpenTrackInfo = { track ->
                            TrackInfoNavigationState
                                .selectedTrack
                                .value =
                                track

                            navController.navigate(
                                Screen.TrackInfo.route
                            )
                        }
                    )

                    Text(
                        text = "More",
                        style =
                        MaterialTheme.typography.caption,
                        color = AuralArcStyle.TextMuted
                    )
                }
            }
        }

        if (
            currentTrack != null
        ) {
            Spacer(
                modifier = Modifier.height(
                    14.dp
                )
            )

            LyricsPreviewCard(
                track = currentTrack,
                currentPositionMs =
                PlayerManager.currentPosition.value,
                onClick = {
                    onShowLyrics(
                        currentTrack
                    )
                }
            )
        }
    }
}

@Composable
private fun LyricsPreviewCard(
    track: MusicTrack,
    currentPositionMs: Long,
    onClick: () -> Unit
) {
    val context =
        LocalContext.current

    val cacheRevision =
        LyricsState.cacheRevision.value

    val result =
        LyricsState.getLyrics(
            track
        )

    val loadingFinished =
        LyricsState.hasFinishedLoading(
            track
        )

    LaunchedEffect(
        track.uri,
        cacheRevision
    ) {
        LyricsState.preloadLyrics(
            context = context.applicationContext,
            track = track
        )
    }

    val parsedDuetLyrics =
        remember(
            result?.text,
            result?.type
        ) {
            if (
                result?.type ==
                EmbeddedLyricsType.DUET_SYNCED
            ) {
                DuetLrcParser.parse(
                    result.text
                )
            } else {
                null
            }
        }

    val duetActiveIndex =
        parsedDuetLyrics
            ?.rows
            ?.indexOfLast { row ->
                currentPositionMs >=
                        row.timeMs
            }
            ?: -1

    val duetPreviewRows =
        remember(
            parsedDuetLyrics,
            duetActiveIndex
        ) {
            val rows =
                parsedDuetLyrics?.rows
                    ?: emptyList()

            if (
                duetActiveIndex >= 0
            ) {
                rows.drop(
                    duetActiveIndex
                ).take(
                    5
                )
            } else {
                rows.take(
                    5
                )
            }
        }

    val normalPreviewLines =
        remember(
            result?.text,
            result?.type,
            currentPositionMs
        ) {
            when {
                result == null ->
                    emptyList()

                result.type ==
                        EmbeddedLyricsType.SYNCED -> {
                    val timedLines =
                        parseLrcLyrics(
                            result.text
                        )

                    val activeIndex =
                        timedLines.indexOfLast { line ->
                            currentPositionMs >=
                                    line.timeMs
                        }

                    if (
                        activeIndex >= 0
                    ) {
                        timedLines
                            .drop(
                                activeIndex
                            )
                            .take(
                                5
                            )
                            .map { line ->
                                line.text
                            }
                    } else {
                        timedLines
                            .take(
                                5
                            )
                            .map { line ->
                                line.text
                            }
                    }
                }

                result.type ==
                        EmbeddedLyricsType.DUET_SYNCED ->
                    emptyList()

                else ->
                    cleanLyricsForDisplay(
                        result.text
                    )
                        .lines()
                        .map { line ->
                            line.trim()
                        }
                        .filter { line ->
                            line.isNotBlank()
                        }
                        .take(
                            5
                        )
            }
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 152.dp,
                max = 270.dp
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 7.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.PurpleBright
            )

            Spacer(
                modifier = Modifier.height(
                    5.dp
                )
            )

            when {
                !loadingFinished ->
                    Text(
                        text = "Loading lyrics…",
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextMuted
                    )

                result?.type ==
                        EmbeddedLyricsType.DUET_SYNCED &&
                        duetPreviewRows.isNotEmpty() -> {
                    duetPreviewRows.forEachIndexed { index, row ->
                        DuetLyricsPreviewRow(
                            row = row,
                            isActive =
                            duetActiveIndex >= 0 &&
                                    index == 0
                        )
                    }
                }

                result?.type ==
                        EmbeddedLyricsType.DUET_SYNCED ->
                    Text(
                        text = "This Duet LRC file does not contain any valid synchronized lyric lines.",
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextMuted
                    )

                normalPreviewLines.isEmpty() ->
                    Text(
                        text = "No lyrics were found.",
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextMuted
                    )

                else ->
                    normalPreviewLines.forEachIndexed { index, line ->
                        Text(
                            text = line,
                            style =
                            if (
                                index == 0
                            ) {
                                MaterialTheme.typography.body1
                            } else {
                                MaterialTheme.typography.body2
                            },
                            fontWeight =
                            if (
                                index == 0
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            color =
                            if (
                                index == 0
                            ) {
                                AuralArcStyle.TextPrimary
                            } else {
                                AuralArcStyle.TextSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
            }
        }
    }
}

@Composable
private fun DuetLyricsPreviewRow(
    row: DuetLyricRow,
    isActive: Boolean
) {
    val inactiveAlpha =
        0.45f

    val fontWeight =
        if (
            isActive
        ) {
            FontWeight.Bold
        } else {
            FontWeight.Normal
        }

    val previewTextStyle =
        MaterialTheme.typography.body2.copy(
            fontSize = 15.sp,
            lineHeight = 19.sp
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 2.dp
            )
    ) {
        if (
            row.sharedText.isNotBlank()
        ) {
            Text(
                text = row.sharedText,
                style = previewTextStyle,
                fontWeight = fontWeight,
                color =
                if (
                    isActive
                ) {
                    AuralArcStyle.TextPrimary
                } else {
                    AuralArcStyle.TextSecondary.copy(
                        alpha = inactiveAlpha
                    )
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (
            row.singerOneText.isNotBlank() ||
            row.singerTwoText.isNotBlank()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 4.dp
                    ),
                horizontalArrangement =
                Arrangement.spacedBy(
                    4.dp
                )
            ) {
                Text(
                    text = row.singerOneText,
                    style = previewTextStyle,
                    fontWeight = fontWeight,
                    color = DuetSingerOneColor.copy(
                        alpha =
                        if (
                            isActive
                        ) {
                            1f
                        } else {
                            inactiveAlpha
                        }
                    ),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start = 6.dp,
                            end = 1.dp
                        )
                )

                Text(
                    text = row.singerTwoText,
                    style = previewTextStyle,
                    fontWeight = fontWeight,
                    color = DuetSingerTwoColor.copy(
                        alpha =
                        if (
                            isActive
                        ) {
                            1f
                        } else {
                            inactiveAlpha
                        }
                    ),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start = 1.dp,
                            end = 6.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun NowPlayingLyricsCard(
    track: MusicTrack
) {
    val context =
        LocalContext.current

    val result =
        LyricsState.getLyrics(
            track
        )

    val loadingFinished =
        LyricsState.hasFinishedLoading(
            track
        )

    val unsyncedScrollState =
        rememberScrollState()

    val cacheRevision =
        LyricsState.cacheRevision.value

    LaunchedEffect(
        track.uri,
        cacheRevision
    ) {
        unsyncedScrollState.scrollTo(
            0
        )

        LyricsState.preloadLyrics(
            context = context.applicationContext,
            track = track
        )
    }

    val cleanedUnsyncedLyrics =
        remember(
            result?.text,
            result?.type
        ) {
            if (
                result != null &&
                result.type !=
                com.example.auralarc.storage
                    .EmbeddedLyricsType.SYNCED
            ) {
                cleanLyricsForDisplay(
                    result.text
                )
            } else {
                ""
            }
        }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    1f
                ),
            shape = AuralArcStyle.CardShape,
            backgroundColor = AuralArcStyle.SurfaceBright,
            elevation = 12.dp
        ) {
            when {
                !loadingFinished -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading lyrics…",
                            style =
                            MaterialTheme.typography.body1,
                            color = AuralArcStyle.TextMuted
                        )
                    }
                }

                result != null &&
                        result.type ==
                        EmbeddedLyricsType.DUET_SYNCED -> {
                    DuetSyncedLyricsText(
                        rawLyrics = result.text,
                        currentPositionMs =
                        PlayerManager.currentPosition.value,
                        onSeekTo = { positionMs ->
                            PlayerManager.seekTo(
                                positionMs
                            )
                        }
                    )
                }

                result != null &&
                        result.type ==
                        com.example.auralarc.storage
                            .EmbeddedLyricsType.SYNCED -> {
                    SyncedLyricsText(
                        rawLyrics = result.text,
                        currentPositionMs =
                        PlayerManager.currentPosition.value,
                        onSeekTo = { positionMs ->
                            PlayerManager.seekTo(
                                positionMs
                            )
                        }
                    )
                }

                cleanedUnsyncedLyrics.isNotBlank() -> {
                    SelectionContainer {
                        Text(
                            text = cleanedUnsyncedLyrics,
                            style =
                            MaterialTheme.typography.body1,
                            color =
                            AuralArcStyle.TextPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(
                                    unsyncedScrollState
                                )
                                .padding(
                                    horizontal = 18.dp,
                                    vertical = 18.dp
                                )
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                24.dp
                            ),
                        contentAlignment =
                        Alignment.Center
                    ) {
                        Text(
                            text =
                            "No lyrics were found. Check your music folders for an LRC or DLRC file, and make sure it matches the name of the song.",
                            style =
                            MaterialTheme.typography.body1,
                            color = AuralArcStyle.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(
                8.dp
            )
        )

        LyricsPlaybackBar(
            track = track
        )
    }
}

@Composable
private fun LyricsPlaybackBar(
    track: MusicTrack
) {
    val context =
        LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                Text(
                    text = track.title,
                    style =
                    MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.artist.ifBlank {
                        "Unknown Artist"
                    },
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AuralArcIconButton(
                onClick = {
                    PlayerManager.previous(
                        context
                    )
                }
            ) {
                Icon(
                    imageVector =
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = AuralArcStyle.TextPrimary
                )
            }

            AuralArcIconButton(
                onClick = {
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        PlayerManager.pause()
                    } else {
                        PlayerManager.resume()
                    }
                }
            ) {
                Icon(
                    imageVector =
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription =
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        "Pause"
                    } else {
                        "Play"
                    },
                    tint = AuralArcStyle.PurpleBright,
                    modifier = Modifier.size(
                        28.dp
                    )
                )
            }

            AuralArcIconButton(
                onClick = {
                    PlayerManager.next(
                        context
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = AuralArcStyle.TextPrimary
                )
            }
        }
    }
}

private data class TimedLyricLine(
    val timeMs: Long,
    val text: String
)

@Composable
private fun DuetSyncedLyricsText(
    rawLyrics: String,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val parsedLyrics =
        remember(
            rawLyrics
        ) {
            DuetLrcParser.parse(
                rawLyrics
            )
        }

    val rows =
        parsedLyrics.rows

    val activeIndex =
        rows.indexOfLast { row ->
            currentPositionMs >=
                    row.timeMs
        }

    val listState =
        rememberLazyListState()

    var automaticScrollInProgress by remember {
        mutableStateOf(
            false
        )
    }

    var userIsManuallyScrolling by remember {
        mutableStateOf(
            false
        )
    }

    LaunchedEffect(
        rawLyrics
    ) {
        automaticScrollInProgress =
            false

        userIsManuallyScrolling =
            false

        listState.scrollToItem(
            0
        )
    }

    LaunchedEffect(
        listState
    ) {
        snapshotFlow {
            listState.isScrollInProgress
        }.collect { scrolling ->
            if (
                scrolling &&
                !automaticScrollInProgress
            ) {
                userIsManuallyScrolling =
                    true
            }

            if (
                !scrolling &&
                userIsManuallyScrolling
            ) {
                delay(
                    1_800L
                )

                if (
                    !listState.isScrollInProgress
                ) {
                    userIsManuallyScrolling =
                        false
                }
            }
        }
    }

    LaunchedEffect(
        activeIndex,
        userIsManuallyScrolling
    ) {
        if (
            userIsManuallyScrolling ||
            activeIndex < 4 ||
            activeIndex !in rows.indices
        ) {
            return@LaunchedEffect
        }

        delay(
            80L
        )

        automaticScrollInProgress =
            true

        try {
            val layoutInfo =
                listState.layoutInfo

            val viewportHeight =
                layoutInfo.viewportEndOffset -
                        layoutInfo.viewportStartOffset

            if (
                viewportHeight > 0
            ) {
                val activeItem =
                    layoutInfo.visibleItemsInfo.firstOrNull { item ->
                        item.index ==
                                activeIndex
                    }

                if (
                    activeItem != null
                ) {
                    val viewportCenter =
                        layoutInfo.viewportStartOffset +
                                viewportHeight / 2

                    val itemCenter =
                        activeItem.offset +
                                activeItem.size / 2

                    val scrollDistance =
                        itemCenter -
                                viewportCenter

                    if (
                        kotlin.math.abs(
                            scrollDistance
                        ) > 4
                    ) {
                        listState.animateScrollBy(
                            scrollDistance.toFloat()
                        )
                    }
                } else {
                    listState.animateScrollToItem(
                        (
                                activeIndex -
                                        3
                                ).coerceAtLeast(
                                0
                            )
                    )
                }
            }
        } finally {
            automaticScrollInProgress =
                false
        }
    }

    if (
        rows.isEmpty()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    24.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This Duet LRC file does not contain any valid synchronized lyric lines.",
                style = MaterialTheme.typography.body1,
                color = AuralArcStyle.TextMuted,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 2.dp,
                    vertical = 10.dp
                ),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 260.dp
            )
        ) {
            itemsIndexed(
                items = rows,
                key = { _, row ->
                    row.timeMs
                }
            ) { index, row ->
                DuetFullLyricsRow(
                    row = row,
                    isActive =
                    index == activeIndex,
                    onClick = {
                        onSeekTo(
                            row.timeMs
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DuetFullLyricsRow(
    row: DuetLyricRow,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val inactiveAlpha =
        0.45f

    val activeFontWeight =
        if (
            isActive
        ) {
            FontWeight.Bold
        } else {
            FontWeight.Normal
        }

    val singerTextStyle =
        MaterialTheme.typography.body1.copy(
            fontSize =
            if (
                isActive
            ) {
                17.sp
            } else {
                16.sp
            },
            lineHeight = 22.sp
        )

    val sharedTextStyle =
        MaterialTheme.typography.body1.copy(
            fontSize =
            if (
                isActive
            ) {
                18.sp
            } else {
                16.sp
            },
            lineHeight = 23.sp
        )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .auralArcClickable {
                onClick()
            }
            .padding(
                horizontal = 8.dp,
                vertical = 10.dp
            )
    ) {
        if (
            row.sharedText.isNotBlank()
        ) {
            Text(
                text = row.sharedText,
                style = sharedTextStyle,
                fontWeight = activeFontWeight,
                color =
                if (
                    isActive
                ) {
                    AuralArcStyle.TextPrimary
                } else {
                    AuralArcStyle.TextSecondary.copy(
                        alpha = inactiveAlpha
                    )
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (
            row.singerOneText.isNotBlank() ||
            row.singerTwoText.isNotBlank()
        ) {
            if (
                row.sharedText.isNotBlank()
            ) {
                Spacer(
                    modifier = Modifier.height(
                        6.dp
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    4.dp
                ),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = row.singerOneText,
                    style = singerTextStyle,
                    fontWeight = activeFontWeight,
                    color = DuetSingerOneColor.copy(
                        alpha =
                        if (
                            isActive
                        ) {
                            1f
                        } else {
                            inactiveAlpha
                        }
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start = 18.dp,
                            end = 1.dp
                        )
                )

                Text(
                    text = row.singerTwoText,
                    style = singerTextStyle,
                    fontWeight = activeFontWeight,
                    color = DuetSingerTwoColor.copy(
                        alpha =
                        if (
                            isActive
                        ) {
                            1f
                        } else {
                            inactiveAlpha
                        }
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start = 1.dp,
                            end = 18.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricsText(
    rawLyrics: String,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit
) {
    val lines =
        remember(
            rawLyrics
        ) {
            parseLrcLyrics(
                rawLyrics
            )
        }

    val activeIndex =
        lines.indexOfLast {
            currentPositionMs >= it.timeMs
        }

    val listState =
        rememberLazyListState()

    val density =
        LocalDensity.current

    val centerLockIndex =
        4

    var automaticScrollInProgress by remember {
        mutableStateOf(
            false
        )
    }

    var userIsManuallyScrolling by remember {
        mutableStateOf(
            false
        )
    }

    LaunchedEffect(
        rawLyrics
    ) {
        /*
         * A changed lyrics document means the song changed.
         * Reset all scrolling state and return the new lyrics
         * to the beginning.
         */
        automaticScrollInProgress =
            false

        userIsManuallyScrolling =
            false

        listState.scrollToItem(
            0
        )
    }

    /*
     * Detect manual scrolling so automatic lyric following does
     * not fight the user while they are reading another section.
     */
    LaunchedEffect(
        listState
    ) {
        snapshotFlow {
            listState.isScrollInProgress
        }.collect { scrolling ->
            if (
                scrolling &&
                !automaticScrollInProgress
            ) {
                userIsManuallyScrolling =
                    true
            }

            if (
                !scrolling &&
                userIsManuallyScrolling
            ) {
                delay(
                    1_800L
                )

                if (
                    !listState.isScrollInProgress
                ) {
                    userIsManuallyScrolling =
                        false
                }
            }
        }
    }

    LaunchedEffect(
        activeIndex,
        userIsManuallyScrolling
    ) {
        if (
            userIsManuallyScrolling ||
            activeIndex < centerLockIndex ||
            activeIndex !in lines.indices
        ) {
            return@LaunchedEffect
        }

        /*
         * Allow the highlighted line to update before calculating
         * the final position.
         */
        delay(
            80L
        )

        val layoutInfo =
            listState.layoutInfo

        val viewportHeight =
            layoutInfo.viewportEndOffset -
                    layoutInfo.viewportStartOffset

        if (
            viewportHeight <= 0
        ) {
            return@LaunchedEffect
        }

        automaticScrollInProgress =
            true

        try {
            val activeItem =
                layoutInfo.visibleItemsInfo.firstOrNull { item ->
                    item.index ==
                            activeIndex
                }

            if (
                activeItem != null
            ) {
                val viewportCenter =
                    layoutInfo.viewportStartOffset +
                            viewportHeight / 2

                val itemCenter =
                    activeItem.offset +
                            activeItem.size / 2

                val scrollDistance =
                    itemCenter -
                            viewportCenter

                if (
                    abs(
                        scrollDistance
                    ) > 4
                ) {
                    listState.animateScrollBy(
                        scrollDistance.toFloat()
                    )
                }
            } else {
                /*
                 * Move close to the active lyric first. Keeping
                 * several lines above it makes the active line land
                 * near the center instead of jumping to the top.
                 */
                val anchorIndex =
                    (
                            activeIndex -
                                    4
                            ).coerceAtLeast(
                            0
                        )

                listState.animateScrollToItem(
                    anchorIndex
                )

                delay(
                    16L
                )

                val refreshedLayout =
                    listState.layoutInfo

                val refreshedActiveItem =
                    refreshedLayout
                        .visibleItemsInfo
                        .firstOrNull { item ->
                            item.index ==
                                    activeIndex
                        }

                if (
                    refreshedActiveItem != null
                ) {
                    val refreshedViewportHeight =
                        refreshedLayout.viewportEndOffset -
                                refreshedLayout.viewportStartOffset

                    val viewportCenter =
                        refreshedLayout.viewportStartOffset +
                                refreshedViewportHeight / 2

                    val itemCenter =
                        refreshedActiveItem.offset +
                                refreshedActiveItem.size / 2

                    val finalScrollDistance =
                        itemCenter -
                                viewportCenter

                    if (
                        abs(
                            finalScrollDistance
                        ) > 4
                    ) {
                        listState.animateScrollBy(
                            finalScrollDistance.toFloat()
                        )
                    }
                }
            }
        } finally {
            automaticScrollInProgress =
                false
        }
    }

    if (
        lines.isEmpty()
    ) {
        Text(
            text = "No synced lyric lines found in this .lrc file.",
            style = MaterialTheme.typography.body1,
            color = AuralArcStyle.TextPrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                )
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 260.dp
            )
        ) {
            itemsIndexed(
                lines
            ) { index, line ->
                val isActive =
                    index == activeIndex

                Text(
                    text = line.text,
                    style =
                    if (
                        isActive
                    ) {
                        MaterialTheme.typography.h6
                    } else {
                        MaterialTheme.typography.body1
                    },
                    fontWeight =
                    if (
                        isActive
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    color =
                    if (
                        isActive
                    ) {
                        AuralArcStyle.PurpleBright
                    } else {
                        AuralArcStyle.TextSecondary
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .auralArcClickable {
                            onSeekTo(
                                line.timeMs
                            )
                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 9.dp
                        ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun parseLrcLyrics(
    rawLyrics: String
): List<TimedLyricLine> {
    val timestampRegex =
        Regex(
            """\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]"""
        )

    val result =
        mutableListOf<TimedLyricLine>()

    rawLyrics.lines().forEach { line ->
        val matches =
            timestampRegex.findAll(
                line
            ).toList()

        if (
            matches.isNotEmpty()
        ) {
            val lyricText =
                line.replace(
                    timestampRegex,
                    ""
                ).trim()

            matches.forEach { match ->
                val minutes =
                    match.groupValues[1].toLongOrNull()
                        ?: 0L

                val seconds =
                    match.groupValues[2].toLongOrNull()
                        ?: 0L

                val fractionRaw =
                    match.groupValues.getOrNull(
                        3
                    ) ?: ""

                val fractionMs =
                    when (
                        fractionRaw.length
                    ) {
                        1 ->
                            fractionRaw.toLongOrNull()
                                ?.times(
                                    100L
                                ) ?: 0L

                        2 ->
                            fractionRaw.toLongOrNull()
                                ?.times(
                                    10L
                                ) ?: 0L

                        3 ->
                            fractionRaw.toLongOrNull()
                                ?: 0L

                        else ->
                            0L
                    }

                val timeMs =
                    minutes * 60_000L +
                            seconds * 1_000L +
                            fractionMs

                if (
                    lyricText.isNotBlank()
                ) {
                    result.add(
                        TimedLyricLine(
                            timeMs = timeMs,
                            text = lyricText
                        )
                    )
                }
            }
        }
    }

    return result.sortedBy {
        it.timeMs
    }
}

private fun cleanLyricsForDisplay(
    text: String
): String {
    return text
        .lines()
        .map { line ->
            line.replace(
                Regex(
                    """\[(?:\d{1,2}:)?\d{1,2}:\d{2}(?:[.:]\d{1,3})?]"""
                ),
                ""
            ).trim()
        }
        .filter {
            it.isNotBlank()
        }
        .joinToString(
            "\n"
        )
}

private fun formatNowPlayingTime(
    duration: Long
): String {
    if (
        duration <= 0L
    ) {
        return "0:00"
    }

    val totalSeconds =
        duration / 1000L

    val minutes =
        totalSeconds / 60L

    val seconds =
        totalSeconds % 60L

    return "$minutes:${seconds.toString().padStart(2, '0')}"
}