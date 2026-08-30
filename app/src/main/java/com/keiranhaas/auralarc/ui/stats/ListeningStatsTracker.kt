package com.keiranhaas.auralarc.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.storage.ListeningStatsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val LISTENING_STATS_FLUSH_MS =
    5_000L

@Composable
fun ListeningStatsTracker() {
    val context =
        LocalContext.current
            .applicationContext

    val statsScope =
        rememberCoroutineScope()

    val writeMutex =
        remember {
            Mutex()
        }

    fun enqueueStatsWrite(
        block: () -> Unit
    ) {
        statsScope.launch(
            Dispatchers.IO
        ) {
            writeMutex.withLock {
                block()
            }
        }
    }

    val currentTitle =
        PlayerManager.currentTitle.value

    val isPlaying =
        PlayerManager.isPlaying.value

    val position =
        PlayerManager.currentPosition.value
            .coerceAtLeast(
                0L
            )

    val duration =
        PlayerManager.duration.value
            .coerceAtLeast(
                0L
            )

    val currentTrack =
        QueueManager.currentTrack()

    var trackedTrack by remember {
        mutableStateOf<MusicTrack?>(
            null
        )
    }

    var trackedUri by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var lastPosition by remember {
        mutableStateOf(
            0L
        )
    }

    var lastDuration by remember {
        mutableStateOf(
            0L
        )
    }

    var pendingListeningMillis by remember {
        mutableStateOf(
            0L
        )
    }

    var playCounted by remember {
        mutableStateOf(
            false
        )
    }

    var completedCounted by remember {
        mutableStateOf(
            false
        )
    }

    var wasPlaying by remember {
        mutableStateOf(
            false
        )
    }

    LaunchedEffect(
        currentTrack?.uri,
        currentTitle
    ) {
        val oldTrack =
            trackedTrack

        val oldUri =
            trackedUri

        val pendingOldListening =
            pendingListeningMillis

        val shouldCountSkip =
            oldTrack != null &&
                    oldUri != null &&
                    oldUri !=
                    currentTrack?.uri &&
                    !completedCounted &&
                    lastPosition >=
                    5_000L &&
                    lastDuration >
                    0L &&
                    lastPosition <
                    (
                            lastDuration *
                                    8L
                            ) /
                    10L

        val newTrack =
            currentTrack

        if (
            oldTrack != null &&
            oldUri != newTrack?.uri &&
            (
                    pendingOldListening >
                            0L ||
                            shouldCountSkip
                    )
        ) {
            enqueueStatsWrite {
                ListeningStatsStore.recordActivityBatch(
                    context = context,
                    track = oldTrack,
                    listeningMillis =
                        pendingOldListening,
                    skipCount =
                        if (
                            shouldCountSkip
                        ) {
                            1
                        } else {
                            0
                        }
                )
            }
        }

        trackedTrack =
            newTrack

        trackedUri =
            newTrack?.uri

        lastPosition =
            position

        lastDuration =
            duration

        pendingListeningMillis =
            0L

        playCounted =
            false

        completedCounted =
            false

        wasPlaying =
            isPlaying

        if (
            newTrack != null
        ) {
            enqueueStatsWrite {
                ListeningStatsStore.recordTrackSeen(
                    context,
                    newTrack
                )
            }
        }
    }

    LaunchedEffect(
        isPlaying,
        position,
        duration,
        currentTrack?.uri
    ) {
        val activeTrack =
            currentTrack
                ?: return@LaunchedEffect

        if (
            trackedUri !=
            activeTrack.uri
        ) {
            return@LaunchedEffect
        }

        val delta =
            position -
                    lastPosition

        if (
            isPlaying &&
            delta in
            1L..15_000L
        ) {
            pendingListeningMillis +=
                delta
        }

        var listeningToFlush =
            0L

        var playDelta =
            0

        var completionDelta =
            0

        val playThreshold =
            when {
                duration <= 0L ->
                    30_000L

                duration < 60_000L ->
                    (
                            duration /
                                    2L
                            ).coerceAtLeast(
                            10_000L
                        )

                else ->
                    30_000L
            }

        if (
            !playCounted &&
            position >=
            playThreshold
        ) {
            playCounted =
                true

            playDelta =
                1
        }

        val completionThreshold =
            if (
                duration > 0L
            ) {
                (
                        duration *
                                8L
                        ) /
                        10L
            } else {
                Long.MAX_VALUE
            }

        if (
            !completedCounted &&
            duration > 0L &&
            position >=
            completionThreshold
        ) {
            completedCounted =
                true

            completionDelta =
                1
        }

        val shouldFlushListening =
            pendingListeningMillis >=
                    LISTENING_STATS_FLUSH_MS ||
                    (
                            !isPlaying &&
                                    wasPlaying &&
                                    pendingListeningMillis >
                                    0L
                            ) ||
                    (
                            completionDelta >
                                    0 &&
                                    pendingListeningMillis >
                                    0L
                            )

        if (
            shouldFlushListening
        ) {
            listeningToFlush =
                pendingListeningMillis

            pendingListeningMillis =
                0L
        }

        if (
            listeningToFlush > 0L ||
            playDelta > 0 ||
            completionDelta > 0
        ) {
            enqueueStatsWrite {
                ListeningStatsStore.recordActivityBatch(
                    context = context,
                    track = activeTrack,
                    listeningMillis =
                        listeningToFlush,
                    playCount =
                        playDelta,
                    completedCount =
                        completionDelta
                )
            }
        }

        lastPosition =
            position

        lastDuration =
            duration

        wasPlaying =
            isPlaying
    }
}