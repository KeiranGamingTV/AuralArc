package com.example.auralarc.scanner

import android.content.Context
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.storage.LibraryCleanupPreferences

object LibraryManager {

    fun loadLibrary(
        context: Context
    ): List<MusicTrack> {
        val mediaStoreTracks =
            MusicScanner.scan(
                context
            )

        val pickedFolderTracks =
            SafAudioScanner.scan(
                context
            )

        val combined =
            mediaStoreTracks +
                    pickedFolderTracks

        return applyCleanupFilters(
            context,
            combined
        ).sortedWith(
            compareBy<MusicTrack> {
                it.artist.lowercase()
            }.thenBy {
                it.album.lowercase()
            }.thenBy {
                it.trackNumber
            }.thenBy {
                it.title.lowercase()
            }
        )
    }

    private fun applyCleanupFilters(
        context: Context,
        tracks: List<MusicTrack>
    ): List<MusicTrack> {
        var filtered =
            tracks

        if (
            LibraryCleanupPreferences.getHideZeroDuration(
                context
            )
        ) {
            filtered =
                filtered.filter {
                    it.duration > 0L
                }
        }

        if (
            LibraryCleanupPreferences.getHideUnknownArtist(
                context
            )
        ) {
            filtered =
                filtered.filterNot {
                    it.artist.equals(
                        "Unknown Artist",
                        ignoreCase = true
                    ) ||
                            it.artist.isBlank()
                }
        }

        if (
            LibraryCleanupPreferences.getHideShortTracks(
                context
            )
        ) {
            val minMs =
                LibraryCleanupPreferences.getMinDurationSeconds(
                    context
                ) * 1000L

            filtered =
                filtered.filter {
                    it.duration >= minMs
                }
        }

        if (
            LibraryCleanupPreferences.getDeduplicate(
                context
            )
        ) {
            filtered =
                filtered.distinctBy { track ->
                    listOf(
                        track.title.trim().lowercase(),
                        track.artist.trim().lowercase(),
                        track.album.trim().lowercase(),
                        track.duration / 1000L
                    ).joinToString(
                        "|"
                    )
                }
        } else {
            filtered =
                filtered.distinctBy {
                    it.uri
                }
        }

        return filtered
    }
}