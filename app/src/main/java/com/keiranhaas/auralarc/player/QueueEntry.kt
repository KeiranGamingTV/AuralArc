package com.keiranhaas.auralarc.player

import com.keiranhaas.auralarc.data.MusicTrack

data class QueueEntry(
    val entryId: String,
    val track: MusicTrack
)