package com.example.auralarc.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.player.QueueManager

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