package com.example.auralarc.player

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.storage.AudioBehaviorPreferences
import com.example.auralarc.storage.ListeningStatsStore
import com.example.auralarc.storage.TrackListeningStats
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmartShuffleState {

    private data class LoadedShuffleStats(
        val level: Int,
        val statsByUri: Map<String, TrackListeningStats>,
        val statsByMetadata: Map<String, TrackListeningStats>
    )

    private val whitespaceRegex =
        Regex(
            """\s+"""
        )

    val level =
        mutableStateOf(
            2
        )

    private var statsByUri =
        emptyMap<String, TrackListeningStats>()

    private var statsByMetadata =
        emptyMap<String, TrackListeningStats>()

    fun refresh(
        context: Context
    ) {
        level.value =
            AudioBehaviorPreferences.getSmartShuffleLevel(
                context
            )

        val stats =
            ListeningStatsStore.getAllTrackStats(
                context
            )

        statsByUri =
            stats.associateBy {
                it.trackUri
            }

        statsByMetadata =
            stats.associateBy {
                metadataKey(
                    title = it.title,
                    artist = it.artist,
                    album = it.album
                )
            }
    }

    suspend fun refreshOffMain(
        context: Context
    ) {
        val applicationContext =
            context.applicationContext

        val loadedStats =
            withContext(
                Dispatchers.IO
            ) {
                val loadedLevel =
                    AudioBehaviorPreferences
                        .getSmartShuffleLevel(
                            applicationContext
                        )
                        .coerceIn(
                            0,
                            4
                        )

                val allStats =
                    ListeningStatsStore
                        .getAllTrackStats(
                            applicationContext
                        )

                LoadedShuffleStats(
                    level =
                    loadedLevel,
                    statsByUri =
                    allStats.associateBy { stat ->
                        stat.trackUri
                    },
                    statsByMetadata =
                    allStats.associateBy { stat ->
                        metadataKey(
                            title = stat.title,
                            artist = stat.artist,
                            album = stat.album
                        )
                    }
                )
            }

        withContext(
            Dispatchers.Main.immediate
        ) {
            level.value =
                loadedStats.level

            statsByUri =
                loadedStats.statsByUri

            statsByMetadata =
                loadedStats.statsByMetadata
        }
    }

    fun smartShuffle(
        tracks: List<MusicTrack>
    ): List<MusicTrack> {
        return smartShuffleItems(
            items = tracks,
            trackProvider = { track ->
                track
            }
        )
    }

    fun smartShuffleEntries(
        entries: List<QueueEntry>
    ): List<QueueEntry> {
        return smartShuffleItems(
            items = entries,
            trackProvider = { entry ->
                entry.track
            }
        )
    }

    private fun <T> smartShuffleItems(
        items: List<T>,
        trackProvider: (T) -> MusicTrack
    ): List<T> {
        if (
            items.size <= 1
        ) {
            return items
        }

        val safeLevel =
            level.value.coerceIn(
                0,
                4
            )

        if (
            safeLevel == 0
        ) {
            return items.shuffled()
        }

        val randomWeight =
            when (
                safeLevel
            ) {
                1 -> 100.0
                2 -> 45.0
                3 -> 15.0
                else -> 3.0
            }

        val leastPlayedWeight =
            when (
                safeLevel
            ) {
                1 -> 0.4
                2 -> 1.0
                3 -> 2.2
                else -> 4.0
            }

        return items
            .map { item ->
                val track =
                    trackProvider(
                        item
                    )

                val stat =
                    statForTrack(
                        track
                    )

                val playScore =
                    if (
                        stat == null
                    ) {
                        0.0
                    } else {
                        stat.playCount.toDouble() *
                                10.0 +
                                stat.completedCount.toDouble() *
                                4.0 +
                                stat.listeningMillis.toDouble() /
                                600_000.0
                    }

                val randomScore =
                    Random.nextDouble(
                        from = 0.0,
                        until = randomWeight
                    )

                val finalScore =
                    playScore *
                            leastPlayedWeight +
                            randomScore

                item to finalScore
            }
            .sortedBy { scoredItem ->
                scoredItem.second
            }
            .map { scoredItem ->
                scoredItem.first
            }
    }

    private fun statForTrack(
        track: MusicTrack
    ): TrackListeningStats? {
        return statsByUri[track.uri]
            ?: statsByMetadata[
                    metadataKey(
                        title = track.title,
                        artist = track.artist,
                        album = track.album
                    )
            ]
    }

    private fun metadataKey(
        title: String,
        artist: String,
        album: String
    ): String {
        return listOf(
            title,
            artist,
            album
        ).joinToString(
            "|"
        ) { value ->
            value.trim()
                .lowercase()
                .replace(
                    whitespaceRegex,
                    " "
                )
        }
    }
}