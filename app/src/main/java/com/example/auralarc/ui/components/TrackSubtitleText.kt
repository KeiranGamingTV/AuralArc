package com.example.auralarc.ui

import com.example.auralarc.data.MusicTrack

fun trackArtistAlbumText(
    track: MusicTrack
): String {
    val artist =
        track.artist
            .trim()
            .ifBlank {
                "Unknown Artist"
            }

    val album =
        track.album
            .trim()
            .ifBlank {
                "Unknown Album"
            }

    return "$artist • $album"
}