package com.keiranhaas.auralarc.player

import android.content.Context
import com.keiranhaas.auralarc.data.LibrarySource
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.TrackIdentity
import com.keiranhaas.auralarc.storage.LibrarySourcePreferences
import com.keiranhaas.auralarc.ui.LibraryRuntimeState
import kotlin.random.Random

/**
 * Chooses songs that are similar to what was just played, so playback
 * can continue when the queue runs out.
 */
object AutoplayManager {

    private const val BATCH_SIZE =
        10

    private const val MAX_PER_ARTIST =
        3

    private const val HISTORY_LIMIT =
        300

    private val recentlyAutoplayedIds =
        LinkedHashSet<String>()

    private data class ScoredTrack(
        val track: MusicTrack,
        val stableId: String,
        val score: Double
    )

    fun getCurrentLibrary(
        context: Context
    ): List<MusicTrack> {
        val source =
            LibrarySourcePreferences.getLibrarySource(
                context
            )

        val tracks =
            if (
                source == LibrarySource.NAVIDROME
            ) {
                LibraryRuntimeState.navidromeTracks.value
            } else {
                LibraryRuntimeState.localTracks.value
            }

        return tracks.orEmpty()
    }

    /**
     * Safe to call off the main thread. [seedTracks] are the songs that
     * just played (the last one is the current song).
     */
    fun pickSimilarTracks(
        seedTracks: List<MusicTrack>,
        queueTracks: List<MusicTrack>,
        library: List<MusicTrack>
    ): List<MusicTrack> {
        if (
            seedTracks.isEmpty() ||
            library.isEmpty()
        ) {
            return emptyList()
        }

        val excludedIds =
            HashSet<String>(
                queueTracks.size * 2
            )

        queueTracks.forEach { track ->
            excludedIds.add(
                TrackIdentity.getStableId(
                    track
                )
            )
        }

        val historyIds =
            synchronized(
                recentlyAutoplayedIds
            ) {
                recentlyAutoplayedIds.toSet()
            }

        val lastSeedIndex =
            seedTracks.lastIndex

        val seedProfiles =
            seedTracks.mapIndexed { index, track ->
                SeedProfile(
                    artist = primaryArtist(
                        track
                    ),
                    genres = genreTokens(
                        track
                    ),
                    album = track.album
                        .trim()
                        .lowercase(),
                    year = track.releaseYear,
                    weight =
                        if (
                            index == lastSeedIndex
                        ) {
                            1.0
                        } else {
                            0.5
                        }
                )
            }

        val scoredTracks =
            ArrayList<ScoredTrack>()

        library.forEach { candidate ->
            val stableId =
                TrackIdentity.getStableId(
                    candidate
                )

            if (
                stableId in excludedIds ||
                stableId in historyIds
            ) {
                return@forEach
            }

            val candidateArtist =
                primaryArtist(
                    candidate
                )

            val candidateGenres =
                genreTokens(
                    candidate
                )

            val candidateAlbum =
                candidate.album
                    .trim()
                    .lowercase()

            var similarity =
                0.0

            seedProfiles.forEach { seed ->
                var pairScore =
                    0.0

                if (
                    candidateArtist.isNotBlank() &&
                    candidateArtist == seed.artist
                ) {
                    pairScore += 4.0
                }

                if (
                    candidateGenres.isNotEmpty() &&
                    candidateGenres.any { genre ->
                        genre in seed.genres
                    }
                ) {
                    pairScore += 3.0
                }

                if (
                    candidateAlbum.isNotBlank() &&
                    candidateAlbum == seed.album
                ) {
                    pairScore += 1.0
                }

                if (
                    candidate.releaseYear > 0 &&
                    seed.year > 0
                ) {
                    val yearGap =
                        kotlin.math.abs(
                            candidate.releaseYear -
                                    seed.year
                        )

                    if (
                        yearGap <= 2
                    ) {
                        pairScore += 1.0
                    } else if (
                        yearGap <= 5
                    ) {
                        pairScore += 0.5
                    }
                }

                similarity +=
                    pairScore * seed.weight
            }

            /*
             * The random nudge keeps repeated Autoplay sessions from
             * always choosing the exact same songs.
             */
            scoredTracks.add(
                ScoredTrack(
                    track = candidate,
                    stableId = stableId,
                    score = similarity +
                            Random.nextDouble() * 1.5
                )
            )
        }

        if (
            scoredTracks.isEmpty()
        ) {
            return emptyList()
        }

        val sortedTracks =
            scoredTracks.sortedByDescending { scored ->
                scored.score
            }

        val picked =
            ArrayList<ScoredTrack>()

        val perArtistCount =
            HashMap<String, Int>()

        for (
        scored in sortedTracks
        ) {
            if (
                picked.size >= BATCH_SIZE
            ) {
                break
            }

            val artist =
                primaryArtist(
                    scored.track
                )

            val count =
                perArtistCount[artist] ?: 0

            if (
                count < MAX_PER_ARTIST
            ) {
                picked.add(
                    scored
                )

                perArtistCount[artist] =
                    count + 1
            }
        }

        if (
            picked.size < BATCH_SIZE
        ) {
            for (
            scored in sortedTracks
            ) {
                if (
                    picked.size >= BATCH_SIZE
                ) {
                    break
                }

                if (
                    picked.none { alreadyPicked ->
                        alreadyPicked.stableId ==
                                scored.stableId
                    }
                ) {
                    picked.add(
                        scored
                    )
                }
            }
        }

        synchronized(
            recentlyAutoplayedIds
        ) {
            picked.forEach { scored ->
                recentlyAutoplayedIds.add(
                    scored.stableId
                )
            }

            while (
                recentlyAutoplayedIds.size >
                HISTORY_LIMIT
            ) {
                val oldest =
                    recentlyAutoplayedIds.first()

                recentlyAutoplayedIds.remove(
                    oldest
                )
            }
        }

        return picked.map { scored ->
            scored.track
        }
    }

    private class SeedProfile(
        val artist: String,
        val genres: Set<String>,
        val album: String,
        val year: Int,
        val weight: Double
    )

    private fun primaryArtist(
        track: MusicTrack
    ): String {
        val source =
            track.albumArtist
                .ifBlank {
                    track.artist
                }

        return source
            .substringBefore(
                ";"
            )
            .trim()
            .lowercase()
    }

    private fun genreTokens(
        track: MusicTrack
    ): Set<String> {
        return track.genre
            .split(
                ";",
                ",",
                "/"
            )
            .map { genre ->
                genre.trim().lowercase()
            }
            .filter { genre ->
                genre.isNotBlank()
            }
            .toSet()
    }
}