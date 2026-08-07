package com.example.auralarc.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.auralarc.data.MusicTrack
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Calendar

enum class ListeningStatsRange(
    val displayName: String
) {
    WEEKLY(
        "Weekly"
    ),
    MONTHLY(
        "Monthly"
    ),
    YEARLY(
        "Yearly"
    ),
    ALL_TIME(
        "All Time"
    )
}

data class TrackListeningStats(
    val trackKey: String,
    val trackUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val playCount: Int,
    val completedCount: Int,
    val skipCount: Int,
    val listeningMillis: Long,
    val firstPlayedAt: Long,
    val lastPlayedAt: Long
)

data class DailyTrackListeningStats(
    val dayStartMillis: Long,
    val trackKey: String,
    val trackUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val playCount: Int,
    val completedCount: Int,
    val skipCount: Int,
    val listeningMillis: Long
)

data class GroupListeningStats(
    val name: String,
    val playCount: Int,
    val listeningMillis: Long
)

data class GenreListeningStats(
    val name: String,
    val listeningMillis: Long,
    val percentage: Float
)

data class ListeningStatsSummary(
    val range: ListeningStatsRange,
    val totalListeningMillis: Long,
    val totalPlays: Int,
    val totalCompleted: Int,
    val totalSkips: Int,
    val uniqueTracks: Int,
    val topTracks: List<TrackListeningStats>,
    val topArtists: List<GroupListeningStats>,
    val topAlbums: List<GroupListeningStats>,
    val genres: List<GenreListeningStats>
)

object ListeningStatsStore {

    private const val PREFS_NAME =
        "listening_stats_store"

    private const val KEY_STATS_JSON =
        "stats_json"

    private const val KEY_LAST_DAILY_PRUNE_DAY =
        "last_daily_prune_day"

    private const val DAILY_KEY_PREFIX =
        "daily_stats_"

    private const val MAX_DAILY_HISTORY_DAYS =
        370

    private data class StatDelta(
        val playCount: Int = 0,
        val completedCount: Int = 0,
        val skipCount: Int = 0,
        val listeningMillis: Long = 0L
    )

    fun getAllTrackStats(
        context: Context
    ): List<TrackListeningStats> {
        return loadLifetimeStats(
            context
        ).values.toList()
    }

    fun recordTrackSeen(
        context: Context,
        track: MusicTrack
    ) {
        val stats =
            loadLifetimeStats(
                context
            )

        val key =
            keyForTrack(
                track.uri
            )

        val existing =
            stats[key]

        stats[key] =
            if (
                existing == null
            ) {
                newLifetimeStat(
                    track = track,
                    trackKey = key,
                    now = System.currentTimeMillis()
                )
            } else {
                existing.withMetadataFrom(
                    track
                )
            }

        saveLifetimeStats(
            context = context,
            stats = stats
        )
    }

    fun recordListeningTime(
        context: Context,
        track: MusicTrack,
        deltaMillis: Long
    ) {
        if (
            deltaMillis <= 0L
        ) {
            return
        }

        recordActivity(
            context = context,
            track = track,
            delta = StatDelta(
                listeningMillis =
                deltaMillis.coerceAtMost(
                    30_000L
                )
            )
        )
    }

    fun recordCountedPlay(
        context: Context,
        track: MusicTrack
    ) {
        recordActivity(
            context = context,
            track = track,
            delta = StatDelta(
                playCount = 1
            )
        )
    }

    fun recordCompletedPlay(
        context: Context,
        track: MusicTrack
    ) {
        recordActivity(
            context = context,
            track = track,
            delta = StatDelta(
                completedCount = 1
            )
        )
    }

    fun recordSkip(
        context: Context,
        track: MusicTrack
    ) {
        recordActivity(
            context = context,
            track = track,
            delta = StatDelta(
                skipCount = 1
            )
        )
    }

    fun getRecentlyListenedTracks(
        context: Context,
        allTracks: List<MusicTrack>,
        limit: Int = 20
    ): List<MusicTrack> {
        if (
            allTracks.isEmpty()
        ) {
            return emptyList()
        }

        val tracksByUri =
            allTracks.associateBy { track ->
                track.uri
            }

        val tracksByKey =
            allTracks.associateBy { track ->
                keyForTrack(
                    track.uri
                )
            }

        return loadLifetimeStats(
            context
        )
            .values
            .asSequence()
            .filter { stat ->
                stat.hasActivity()
            }
            .sortedByDescending { stat ->
                stat.lastPlayedAt
            }
            .mapNotNull { stat ->
                tracksByKey[stat.trackKey]
                    ?: tracksByUri[stat.trackUri]
            }
            .distinctBy { track ->
                track.uri
            }
            .take(
                limit
            )
            .toList()
    }

    fun refreshTrackMetadata(
        context: Context,
        tracks: List<MusicTrack>
    ) {
        if (
            tracks.isEmpty()
        ) {
            return
        }

        val tracksByKey =
            tracks.associateBy { track ->
                keyForTrack(
                    track.uri
                )
            }

        val tracksByUri =
            tracks.associateBy { track ->
                track.uri
            }

        val prefs =
            preferences(
                context
            )

        val editor =
            prefs.edit()

        val lifetime =
            loadLifetimeStats(
                context
            )

        var lifetimeChanged =
            false

        lifetime.entries.forEach { entry ->
            val old =
                entry.value

            val track =
                tracksByKey[old.trackKey]
                    ?: tracksByUri[old.trackUri]
                    ?: return@forEach

            val updated =
                old.withMetadataFrom(
                    track
                )

            if (
                updated != old
            ) {
                entry.setValue(
                    updated
                )

                lifetimeChanged =
                    true
            }
        }

        if (
            lifetimeChanged
        ) {
            editor.putString(
                KEY_STATS_JSON,
                lifetimeJson(
                    lifetime
                ).toString()
            )
        }

        prefs.all.keys
            .filter { key ->
                key.startsWith(
                    DAILY_KEY_PREFIX
                )
            }
            .forEach dayLoop@ { storageKey ->
                val dayStart =
                    storageKey
                        .removePrefix(
                            DAILY_KEY_PREFIX
                        )
                        .toLongOrNull()
                        ?: return@dayLoop

                val daily =
                    loadDailyStatsForDay(
                        prefs = prefs,
                        dayStartMillis = dayStart
                    )

                var changed =
                    false

                daily.entries.forEach dailyEntry@ { entry ->
                    val old =
                        entry.value

                    val track =
                        tracksByKey[old.trackKey]
                            ?: tracksByUri[old.trackUri]
                            ?: return@dailyEntry

                    val updated =
                        old.withMetadataFrom(
                            track
                        )

                    if (
                        updated != old
                    ) {
                        entry.setValue(
                            updated
                        )

                        changed =
                            true
                    }
                }

                if (
                    changed
                ) {
                    editor.putString(
                        storageKey,
                        dailyJson(
                            daily
                        ).toString()
                    )
                }
            }

        editor.apply()
    }

    fun getSummary(
        context: Context,
        range: ListeningStatsRange
    ): ListeningStatsSummary {
        val stats =
            when (
                range
            ) {
                ListeningStatsRange.ALL_TIME -> {
                    loadLifetimeStats(
                        context
                    ).values.toList()
                }

                else -> {
                    aggregateDailyStats(
                        loadDailyStatsForRange(
                            context = context,
                            range = range
                        )
                    )
                }
            }
                .filter { stat ->
                    stat.hasActivity()
                }

        val totalListeningMillis =
            stats.sumOf { stat ->
                stat.listeningMillis
            }

        return ListeningStatsSummary(
            range = range,
            totalListeningMillis =
            totalListeningMillis,
            totalPlays =
            stats.sumOf { stat ->
                stat.playCount
            },
            totalCompleted =
            stats.sumOf { stat ->
                stat.completedCount
            },
            totalSkips =
            stats.sumOf { stat ->
                stat.skipCount
            },
            uniqueTracks =
            stats.size,
            topTracks =
            stats
                .sortedWith(
                    compareByDescending<TrackListeningStats> { stat ->
                        stat.playCount
                    }.thenByDescending { stat ->
                        stat.listeningMillis
                    }
                )
                .take(
                    5
                ),
            topArtists =
            groupStats(
                stats
            ) { stat ->
                stat.artist.ifBlank {
                    "Unknown Artist"
                }
            },
            topAlbums =
            groupStats(
                stats
            ) { stat ->
                stat.album.ifBlank {
                    "Unknown Album"
                }
            },
            genres =
            buildGenreStats(
                stats = stats,
                totalListeningMillis =
                totalListeningMillis
            )
        )
    }

    private fun recordActivity(
        context: Context,
        track: MusicTrack,
        delta: StatDelta
    ) {
        val now =
            System.currentTimeMillis()

        val trackKey =
            keyForTrack(
                track.uri
            )

        val dayStart =
            startOfDayMillis(
                now
            )

        val prefs =
            preferences(
                context
            )

        pruneOldDailyKeysIfNeeded(
            prefs = prefs,
            todayStart = dayStart
        )

        val lifetime =
            loadLifetimeStats(
                context
            )

        val oldLifetime =
            lifetime[trackKey]
                ?.withMetadataFrom(
                    track
                )
                ?: newLifetimeStat(
                    track = track,
                    trackKey = trackKey,
                    now = now
                )

        lifetime[trackKey] =
            oldLifetime.copy(
                playCount =
                oldLifetime.playCount +
                        delta.playCount,
                completedCount =
                oldLifetime.completedCount +
                        delta.completedCount,
                skipCount =
                oldLifetime.skipCount +
                        delta.skipCount,
                listeningMillis =
                oldLifetime.listeningMillis +
                        delta.listeningMillis,
                lastPlayedAt = now
            )

        val daily =
            loadDailyStatsForDay(
                prefs = prefs,
                dayStartMillis = dayStart
            )

        val oldDaily =
            daily[trackKey]
                ?.withMetadataFrom(
                    track
                )
                ?: newDailyStat(
                    track = track,
                    trackKey = trackKey,
                    dayStartMillis = dayStart
                )

        daily[trackKey] =
            oldDaily.copy(
                playCount =
                oldDaily.playCount +
                        delta.playCount,
                completedCount =
                oldDaily.completedCount +
                        delta.completedCount,
                skipCount =
                oldDaily.skipCount +
                        delta.skipCount,
                listeningMillis =
                oldDaily.listeningMillis +
                        delta.listeningMillis
            )

        prefs.edit()
            .putString(
                KEY_STATS_JSON,
                lifetimeJson(
                    lifetime
                ).toString()
            )
            .putString(
                dailyStorageKey(
                    dayStart
                ),
                dailyJson(
                    daily
                ).toString()
            )
            .apply()
    }

    private fun newLifetimeStat(
        track: MusicTrack,
        trackKey: String,
        now: Long
    ): TrackListeningStats {
        return TrackListeningStats(
            trackKey = trackKey,
            trackUri = track.uri,
            title = track.title,
            artist = track.artist,
            album = track.album,
            genre = normalizeGenre(
                track.genre
            ),
            playCount = 0,
            completedCount = 0,
            skipCount = 0,
            listeningMillis = 0L,
            firstPlayedAt = now,
            lastPlayedAt = 0L
        )
    }

    private fun newDailyStat(
        track: MusicTrack,
        trackKey: String,
        dayStartMillis: Long
    ): DailyTrackListeningStats {
        return DailyTrackListeningStats(
            dayStartMillis =
            dayStartMillis,
            trackKey = trackKey,
            trackUri = track.uri,
            title = track.title,
            artist = track.artist,
            album = track.album,
            genre = normalizeGenre(
                track.genre
            ),
            playCount = 0,
            completedCount = 0,
            skipCount = 0,
            listeningMillis = 0L
        )
    }

    private fun TrackListeningStats.withMetadataFrom(
        track: MusicTrack
    ): TrackListeningStats {
        return copy(
            trackUri = track.uri,
            title = track.title.ifBlank {
                title
            },
            artist = track.artist.ifBlank {
                artist
            },
            album = track.album.ifBlank {
                album
            },
            genre =
            track.genre
                .trim()
                .takeIf { value ->
                    value.isNotBlank()
                }
                ?: genre
        )
    }

    private fun DailyTrackListeningStats.withMetadataFrom(
        track: MusicTrack
    ): DailyTrackListeningStats {
        return copy(
            trackUri = track.uri,
            title = track.title.ifBlank {
                title
            },
            artist = track.artist.ifBlank {
                artist
            },
            album = track.album.ifBlank {
                album
            },
            genre =
            track.genre
                .trim()
                .takeIf { value ->
                    value.isNotBlank()
                }
                ?: genre
        )
    }

    private fun TrackListeningStats.hasActivity(): Boolean {
        return playCount > 0 ||
                completedCount > 0 ||
                skipCount > 0 ||
                listeningMillis > 0L
    }

    private fun loadDailyStatsForRange(
        context: Context,
        range: ListeningStatsRange
    ): List<DailyTrackListeningStats> {
        val prefs =
            preferences(
                context
            )

        val now =
            System.currentTimeMillis()

        val start =
            rangeStartMillis(
                range = range,
                now = now
            )

        val end =
            startOfDayMillis(
                now
            )

        val result =
            mutableListOf<DailyTrackListeningStats>()

        val calendar =
            Calendar.getInstance().apply {
                timeInMillis =
                    start
            }

        while (
            calendar.timeInMillis <= end
        ) {
            result.addAll(
                loadDailyStatsForDay(
                    prefs = prefs,
                    dayStartMillis =
                    calendar.timeInMillis
                ).values
            )

            calendar.add(
                Calendar.DAY_OF_YEAR,
                1
            )
        }

        return result
    }

    private fun aggregateDailyStats(
        dailyStats: List<DailyTrackListeningStats>
    ): List<TrackListeningStats> {
        return dailyStats
            .groupBy { stat ->
                stat.trackKey
            }
            .mapNotNull { entry ->
                val trackKey =
                    entry.key

                val values =
                    entry.value

                val latest =
                    values.maxByOrNull { stat ->
                        stat.dayStartMillis
                    }
                        ?: return@mapNotNull null

                TrackListeningStats(
                    trackKey = trackKey,
                    trackUri = latest.trackUri,
                    title = latest.title,
                    artist = latest.artist,
                    album = latest.album,
                    genre = latest.genre,
                    playCount =
                    values.sumOf { stat ->
                        stat.playCount
                    },
                    completedCount =
                    values.sumOf { stat ->
                        stat.completedCount
                    },
                    skipCount =
                    values.sumOf { stat ->
                        stat.skipCount
                    },
                    listeningMillis =
                    values.sumOf { stat ->
                        stat.listeningMillis
                    },
                    firstPlayedAt =
                    values.minOf { stat ->
                        stat.dayStartMillis
                    },
                    lastPlayedAt =
                    values.maxOf { stat ->
                        stat.dayStartMillis
                    }
                )
            }
    }

    private fun groupStats(
        stats: List<TrackListeningStats>,
        nameProvider: (TrackListeningStats) -> String
    ): List<GroupListeningStats> {
        return stats
            .groupBy { stat ->
                nameProvider(
                    stat
                )
            }
            .map { entry ->
                GroupListeningStats(
                    name = entry.key,
                    playCount =
                    entry.value.sumOf { stat ->
                        stat.playCount
                    },
                    listeningMillis =
                    entry.value.sumOf { stat ->
                        stat.listeningMillis
                    }
                )
            }
            .sortedWith(
                compareByDescending<GroupListeningStats> { stat ->
                    stat.playCount
                }.thenByDescending { stat ->
                    stat.listeningMillis
                }
            )
            .take(
                5
            )
    }

    private fun buildGenreStats(
        stats: List<TrackListeningStats>,
        totalListeningMillis: Long
    ): List<GenreListeningStats> {
        if (
            totalListeningMillis <= 0L
        ) {
            return emptyList()
        }

        return stats
            .filter { stat ->
                stat.listeningMillis > 0L
            }
            .groupBy { stat ->
                normalizeGenre(
                    stat.genre
                )
            }
            .map { entry ->
                val genreMillis =
                    entry.value.sumOf { stat ->
                        stat.listeningMillis
                    }

                GenreListeningStats(
                    name = entry.key,
                    listeningMillis =
                    genreMillis,
                    percentage =
                    genreMillis.toFloat() /
                            totalListeningMillis.toFloat() *
                            100f
                )
            }
            .sortedByDescending { stat ->
                stat.listeningMillis
            }
    }

    private fun normalizeGenre(
        value: String
    ): String {
        return value
            .trim()
            .takeIf { genre ->
                genre.isNotBlank()
            }
            ?: "Unknown"
    }

    private fun rangeStartMillis(
        range: ListeningStatsRange,
        now: Long
    ): Long {
        val days =
            when (
                range
            ) {
                ListeningStatsRange.WEEKLY ->
                    7

                ListeningStatsRange.MONTHLY ->
                    30

                ListeningStatsRange.YEARLY ->
                    365

                ListeningStatsRange.ALL_TIME ->
                    return Long.MIN_VALUE
            }

        return Calendar.getInstance().apply {
            timeInMillis =
                now

            set(
                Calendar.HOUR_OF_DAY,
                0
            )
            set(
                Calendar.MINUTE,
                0
            )
            set(
                Calendar.SECOND,
                0
            )
            set(
                Calendar.MILLISECOND,
                0
            )
            add(
                Calendar.DAY_OF_YEAR,
                -(days - 1)
            )
        }.timeInMillis
    }

    private fun startOfDayMillis(
        timeMillis: Long
    ): Long {
        return Calendar.getInstance().apply {
            this.timeInMillis =
                timeMillis

            set(
                Calendar.HOUR_OF_DAY,
                0
            )
            set(
                Calendar.MINUTE,
                0
            )
            set(
                Calendar.SECOND,
                0
            )
            set(
                Calendar.MILLISECOND,
                0
            )
        }.timeInMillis
    }

    private fun pruneOldDailyKeysIfNeeded(
        prefs: SharedPreferences,
        todayStart: Long
    ) {
        if (
            prefs.getLong(
                KEY_LAST_DAILY_PRUNE_DAY,
                0L
            ) == todayStart
        ) {
            return
        }

        val oldestAllowed =
            Calendar.getInstance().apply {
                timeInMillis =
                    todayStart

                add(
                    Calendar.DAY_OF_YEAR,
                    -(MAX_DAILY_HISTORY_DAYS - 1)
                )
            }.timeInMillis

        val editor =
            prefs.edit()

        prefs.all.keys
            .filter { key ->
                key.startsWith(
                    DAILY_KEY_PREFIX
                )
            }
            .forEach { key ->
                val dayStart =
                    key
                        .removePrefix(
                            DAILY_KEY_PREFIX
                        )
                        .toLongOrNull()

                if (
                    dayStart != null &&
                    dayStart < oldestAllowed
                ) {
                    editor.remove(
                        key
                    )
                }
            }

        editor
            .putLong(
                KEY_LAST_DAILY_PRUNE_DAY,
                todayStart
            )
            .apply()
    }

    private fun loadLifetimeStats(
        context: Context
    ): MutableMap<String, TrackListeningStats> {
        val raw =
            preferences(
                context
            ).getString(
                KEY_STATS_JSON,
                "[]"
            ) ?: "[]"

        val result =
            mutableMapOf<String, TrackListeningStats>()

        try {
            val array =
                JSONArray(
                    raw
                )

            for (
            index in 0 until array.length()
            ) {
                val item =
                    array.optJSONObject(
                        index
                    ) ?: continue

                val stat =
                    TrackListeningStats(
                        trackKey =
                        item.optString(
                            "trackKey",
                            ""
                        ),
                        trackUri =
                        item.optString(
                            "trackUri",
                            ""
                        ),
                        title =
                        item.optString(
                            "title",
                            "Unknown Title"
                        ),
                        artist =
                        item.optString(
                            "artist",
                            "Unknown Artist"
                        ),
                        album =
                        item.optString(
                            "album",
                            "Unknown Album"
                        ),
                        genre =
                        item.optString(
                            "genre",
                            "Unknown"
                        ),
                        playCount =
                        item.optInt(
                            "playCount",
                            0
                        ),
                        completedCount =
                        item.optInt(
                            "completedCount",
                            0
                        ),
                        skipCount =
                        item.optInt(
                            "skipCount",
                            0
                        ),
                        listeningMillis =
                        item.optLong(
                            "listeningMillis",
                            0L
                        ),
                        firstPlayedAt =
                        item.optLong(
                            "firstPlayedAt",
                            0L
                        ),
                        lastPlayedAt =
                        item.optLong(
                            "lastPlayedAt",
                            0L
                        )
                    )

                if (
                    stat.trackKey.isNotBlank()
                ) {
                    result[stat.trackKey] =
                        stat
                }
            }
        } catch (_: Exception) {
            return mutableMapOf()
        }

        return result
    }

    private fun loadDailyStatsForDay(
        prefs: SharedPreferences,
        dayStartMillis: Long
    ): MutableMap<String, DailyTrackListeningStats> {
        val raw =
            prefs.getString(
                dailyStorageKey(
                    dayStartMillis
                ),
                "[]"
            ) ?: "[]"

        val result =
            mutableMapOf<String, DailyTrackListeningStats>()

        try {
            val array =
                JSONArray(
                    raw
                )

            for (
            index in 0 until array.length()
            ) {
                val item =
                    array.optJSONObject(
                        index
                    ) ?: continue

                val stat =
                    DailyTrackListeningStats(
                        dayStartMillis =
                        dayStartMillis,
                        trackKey =
                        item.optString(
                            "trackKey",
                            ""
                        ),
                        trackUri =
                        item.optString(
                            "trackUri",
                            ""
                        ),
                        title =
                        item.optString(
                            "title",
                            "Unknown Title"
                        ),
                        artist =
                        item.optString(
                            "artist",
                            "Unknown Artist"
                        ),
                        album =
                        item.optString(
                            "album",
                            "Unknown Album"
                        ),
                        genre =
                        item.optString(
                            "genre",
                            "Unknown"
                        ),
                        playCount =
                        item.optInt(
                            "playCount",
                            0
                        ),
                        completedCount =
                        item.optInt(
                            "completedCount",
                            0
                        ),
                        skipCount =
                        item.optInt(
                            "skipCount",
                            0
                        ),
                        listeningMillis =
                        item.optLong(
                            "listeningMillis",
                            0L
                        )
                    )

                if (
                    stat.trackKey.isNotBlank()
                ) {
                    result[stat.trackKey] =
                        stat
                }
            }
        } catch (_: Exception) {
            return mutableMapOf()
        }

        return result
    }

    private fun saveLifetimeStats(
        context: Context,
        stats: Map<String, TrackListeningStats>
    ) {
        preferences(
            context
        )
            .edit()
            .putString(
                KEY_STATS_JSON,
                lifetimeJson(
                    stats
                ).toString()
            )
            .apply()
    }

    private fun lifetimeJson(
        stats: Map<String, TrackListeningStats>
    ): JSONArray {
        val array =
            JSONArray()

        stats.values.forEach { stat ->
            array.put(
                JSONObject().apply {
                    put(
                        "trackKey",
                        stat.trackKey
                    )
                    put(
                        "trackUri",
                        stat.trackUri
                    )
                    put(
                        "title",
                        stat.title
                    )
                    put(
                        "artist",
                        stat.artist
                    )
                    put(
                        "album",
                        stat.album
                    )
                    put(
                        "genre",
                        stat.genre
                    )
                    put(
                        "playCount",
                        stat.playCount
                    )
                    put(
                        "completedCount",
                        stat.completedCount
                    )
                    put(
                        "skipCount",
                        stat.skipCount
                    )
                    put(
                        "listeningMillis",
                        stat.listeningMillis
                    )
                    put(
                        "firstPlayedAt",
                        stat.firstPlayedAt
                    )
                    put(
                        "lastPlayedAt",
                        stat.lastPlayedAt
                    )
                }
            )
        }

        return array
    }

    private fun dailyJson(
        stats: Map<String, DailyTrackListeningStats>
    ): JSONArray {
        val array =
            JSONArray()

        stats.values.forEach { stat ->
            array.put(
                JSONObject().apply {
                    put(
                        "trackKey",
                        stat.trackKey
                    )
                    put(
                        "trackUri",
                        stat.trackUri
                    )
                    put(
                        "title",
                        stat.title
                    )
                    put(
                        "artist",
                        stat.artist
                    )
                    put(
                        "album",
                        stat.album
                    )
                    put(
                        "genre",
                        stat.genre
                    )
                    put(
                        "playCount",
                        stat.playCount
                    )
                    put(
                        "completedCount",
                        stat.completedCount
                    )
                    put(
                        "skipCount",
                        stat.skipCount
                    )
                    put(
                        "listeningMillis",
                        stat.listeningMillis
                    )
                }
            )
        }

        return array
    }

    private fun preferences(
        context: Context
    ): SharedPreferences {
        return context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
    }

    private fun dailyStorageKey(
        dayStartMillis: Long
    ): String {
        return "$DAILY_KEY_PREFIX$dayStartMillis"
    }

    private fun keyForTrack(
        trackUri: String
    ): String {
        val bytes =
            MessageDigest
                .getInstance(
                    "MD5"
                )
                .digest(
                    trackUri.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return bytes.joinToString(
            separator = ""
        ) { byte ->
            "%02x".format(
                byte
            )
        }
    }
}