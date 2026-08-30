package com.keiranhaas.auralarc.scanner

import android.content.Context
import com.keiranhaas.auralarc.data.LibrarySource
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.storage.LibraryCacheStore
import com.keiranhaas.auralarc.storage.LibraryCleanupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object LibraryManager {

    suspend fun loadLibrary(
        context: Context
    ): List<MusicTrack> =
        coroutineScope {
            val appContext =
                context.applicationContext

            /*
             * Use the last successful local-library cache as an
             * incremental scan index.
             *
             * Unchanged files can reuse their expensive codec,
             * bitrate, sample-rate, bit-depth and artwork data.
             */
            val cachedTracksByUri =
                withContext(
                    Dispatchers.IO
                ) {
                    LibraryCacheStore.loadTracks(
                        context = appContext,
                        source = LibrarySource.LOCAL
                    )
                        .orEmpty()
                        .associateBy { track ->
                            track.uri
                        }
                }

            /*
             * MediaStore and SAF are independent data sources.
             * Scan them concurrently instead of serially.
             */
            val mediaStoreScan =
                async(
                    Dispatchers.IO
                ) {
                    MusicScanner.scan(
                        context = appContext,
                        cachedTracksByUri =
                            cachedTracksByUri
                    )
                }

            val safScan =
                async(
                    Dispatchers.IO
                ) {
                    SafAudioScanner.scan(
                        context = appContext,
                        cachedTracksByUri =
                            cachedTracksByUri
                    )
                }

            val combined =
                mediaStoreScan.await() +
                        safScan.await()

            applyCleanupFilters(
                context = appContext,
                tracks = combined
            )
                .sortedWith(
                    compareBy<MusicTrack> {
                        it.artist.lowercase()
                    }.thenBy {
                        it.album.lowercase()
                    }.thenBy {
                        if (
                            it.discNumber > 0
                        ) {
                            it.discNumber
                        } else {
                            Int.MAX_VALUE
                        }
                    }.thenBy {
                        if (
                            it.trackNumber > 0
                        ) {
                            it.trackNumber
                        } else {
                            Int.MAX_VALUE
                        }
                    }.thenBy {
                        it.title.lowercase()
                    }
                )
        }

    private fun applyCleanupFilters(
        context: Context,
        tracks: List<MusicTrack>
    ): List<MusicTrack> {
        val hideZeroDuration =
            LibraryCleanupPreferences.getHideZeroDuration(
                context
            )

        val hideUnknownArtist =
            LibraryCleanupPreferences.getHideUnknownArtist(
                context
            )

        val hideShortTracks =
            LibraryCleanupPreferences.getHideShortTracks(
                context
            )

        val minimumDurationMs =
            LibraryCleanupPreferences.getMinDurationSeconds(
                context
            ) * 1000L

        val filtered =
            tracks
                .asSequence()
                .filter { track ->
                    !hideZeroDuration ||
                            track.duration > 0L
                }
                .filter { track ->
                    !hideUnknownArtist ||
                            (
                                    track.artist.isNotBlank() &&
                                            !track.artist.equals(
                                                "Unknown Artist",
                                                ignoreCase = true
                                            )
                                    )
                }
                .filter { track ->
                    !hideShortTracks ||
                            track.duration >=
                            minimumDurationMs
                }
                .toList()

        return if (
            LibraryCleanupPreferences.getDeduplicate(
                context
            )
        ) {
            filtered.distinctBy { track ->
                buildString {
                    append(
                        track.title
                            .trim()
                            .lowercase()
                    )

                    append(
                        '|'
                    )

                    append(
                        track.artist
                            .trim()
                            .lowercase()
                    )

                    append(
                        '|'
                    )

                    append(
                        track.album
                            .trim()
                            .lowercase()
                    )

                    append(
                        '|'
                    )

                    append(
                        track.duration / 1000L
                    )
                }
            }
        } else {
            filtered.distinctBy { track ->
                track.uri
            }
        }
    }
}