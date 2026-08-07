package com.example.auralarc.player

import com.example.auralarc.data.MusicTrack

data class QueueEntry(
    val entryId: String,
    val track: MusicTrack
)