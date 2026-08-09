package com.keiranhaas.auralarc.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager

@Composable
fun LyricsPreloadTracker() {
    val context =
        LocalContext.current

    val currentTrack =
        QueueManager.currentTrack()

    val isPlaying =
        PlayerManager.isPlaying.value

    LaunchedEffect(
        currentTrack?.uri,
        isPlaying
    ) {
        if (
            currentTrack != null &&
            isPlaying &&
            LyricsState.isNavidromeTrack(
                currentTrack.uri
            )
        ) {
            LyricsState.preloadLyrics(
                context = context.applicationContext,
                track = currentTrack
            )
        }
    }
}