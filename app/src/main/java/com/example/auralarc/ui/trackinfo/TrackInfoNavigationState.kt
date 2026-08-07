package com.example.auralarc.ui

import androidx.compose.runtime.mutableStateOf
import com.example.auralarc.data.MusicTrack

object TrackInfoNavigationState {
    val selectedTrack =
        mutableStateOf<MusicTrack?>(
            null
        )
}