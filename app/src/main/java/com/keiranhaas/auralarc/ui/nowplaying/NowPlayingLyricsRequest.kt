package com.keiranhaas.auralarc.ui

import androidx.compose.runtime.mutableStateOf
import com.keiranhaas.auralarc.data.MusicTrack

object NowPlayingLyricsRequest {
    val requestedTrack =
        mutableStateOf<MusicTrack?>(
            null
        )

    val shouldOpen =
        mutableStateOf(
            false
        )

    fun request(
        track: MusicTrack
    ) {
        requestedTrack.value =
            track

        shouldOpen.value =
            true
    }

    fun clear() {
        requestedTrack.value =
            null

        shouldOpen.value =
            false
    }
}