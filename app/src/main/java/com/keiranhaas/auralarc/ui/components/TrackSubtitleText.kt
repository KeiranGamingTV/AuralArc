package com.keiranhaas.auralarc.ui

import com.keiranhaas.auralarc.data.MusicTrack

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