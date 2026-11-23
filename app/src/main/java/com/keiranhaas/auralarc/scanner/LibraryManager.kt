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
import android.os.SystemClock
import android.util.Log

object LibraryManager {

    suspend fun loadLibrary(
        context: Context
    ): List<MusicTrack> =
        coroutineScope {
            val startedAt =
                SystemClock.elapsedRealtime()

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

            val result =
                applyCleanupFilters(
                    context = appContext,
                    tracks = combined
                )
                    .map { track ->
                        TrackSortKey(
                            track
                        )
                    }
                    .sorted()
                    .map { key ->
                        key.track
                    }

            Log.d(
                "AuralArc",
                "Library scan finished: ${result.size} tracks in " +
                        "${SystemClock.elapsedRealtime() - startedAt} ms"
            )

            result
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

private class TrackSortKey(
    val track: MusicTrack
) : Comparable<TrackSortKey> {

    private val artist =
        track.artist.lowercase()

    private val album =
        track.album.lowercase()

    private val disc =
        if (
            track.discNumber > 0
        ) {
            track.discNumber
        } else {
            Int.MAX_VALUE
        }

    private val number =
        if (
            track.trackNumber > 0
        ) {
            track.trackNumber
        } else {
            Int.MAX_VALUE
        }

    private val title =
        track.title.lowercase()

    override fun compareTo(
        other: TrackSortKey
    ): Int {
        var result =
            artist.compareTo(other.artist)

        if (result != 0) return result

        result =
            album.compareTo(other.album)

        if (result != 0) return result

        result =
            disc.compareTo(other.disc)

        if (result != 0) return result

        result =
            number.compareTo(other.number)

        if (result != 0) return result

        result =
            title.compareTo(other.title)

        if (result != 0) return result

        return track.uri.compareTo(
            other.track.uri
        )
    }
}