package com.keiranhaas.auralarc.ui

import androidx.compose.runtime.mutableStateOf
import com.keiranhaas.auralarc.data.MusicTrack

object TrackInfoNavigationState {
    val selectedTrack =
        mutableStateOf<MusicTrack?>(
            null
        )
}