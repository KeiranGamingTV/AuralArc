package com.example.auralarc.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.player.QueueManager
import com.example.auralarc.storage.ListeningStatsStore

@Composable
fun ListeningStatsTracker() {
    val context =
        LocalContext.current

    val currentTitle =
        PlayerManager.currentTitle.value

    val isPlaying =
        PlayerManager.isPlaying.value

    val position =
        PlayerManager.currentPosition.value.coerceAtLeast(
            0L
        )

    val duration =
        PlayerManager.duration.value.coerceAtLeast(
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

    LaunchedEffect(
        currentTrack?.uri,
        currentTitle
    ) {
        val oldTrack =
            trackedTrack

        val oldUri =
            trackedUri

        if (
            oldTrack != null &&
            oldUri != null &&
            oldUri != currentTrack?.uri
        ) {
            val shouldCountSkip =
                !completedCounted &&
                        lastPosition >= 5000L &&
                        lastDuration > 0L &&
                        lastPosition < ((lastDuration * 8L) / 10L)

            if (
                shouldCountSkip
            ) {
                ListeningStatsStore.recordSkip(
                    context,
                    oldTrack
                )
            }
        }

        trackedTrack =
            currentTrack

        trackedUri =
            currentTrack?.uri

        lastPosition =
            position

        lastDuration =
            duration

        playCounted =
            false

        completedCounted =
            false

        if (
            currentTrack != null
        ) {
            ListeningStatsStore.recordTrackSeen(
                context,
                currentTrack
            )
        }
    }

    LaunchedEffect(
        isPlaying,
        position,
        duration,
        currentTrack?.uri
    ) {
        val activeTrack =
            currentTrack ?: return@LaunchedEffect

        if (
            trackedUri != activeTrack.uri
        ) {
            return@LaunchedEffect
        }

        val delta =
            position - lastPosition

        if (
            isPlaying &&
            delta in 1L..15000L
        ) {
            ListeningStatsStore.recordListeningTime(
                context = context,
                track = activeTrack,
                deltaMillis = delta
            )
        }

        val playThreshold =
            when {
                duration <= 0L ->
                    30000L

                duration < 60000L ->
                    (duration / 2L).coerceAtLeast(
                        10000L
                    )

                else ->
                    30000L
            }

        if (
            !playCounted &&
            position >= playThreshold
        ) {
            ListeningStatsStore.recordCountedPlay(
                context,
                activeTrack
            )

            playCounted =
                true
        }

        val completionThreshold =
            if (
                duration > 0L
            ) {
                (duration * 8L) / 10L
            } else {
                Long.MAX_VALUE
            }

        if (
            !completedCounted &&
            duration > 0L &&
            position >= completionThreshold
        ) {
            ListeningStatsStore.recordCompletedPlay(
                context,
                activeTrack
            )

            completedCounted =
                true
        }

        lastPosition =
            position

        lastDuration =
            duration
    }
}