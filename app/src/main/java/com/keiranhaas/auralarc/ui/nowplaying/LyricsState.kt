package com.keiranhaas.auralarc.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.navidrome.NavidromeClient
import com.keiranhaas.auralarc.navidrome.NavidromePreferences
import com.keiranhaas.auralarc.storage.EmbeddedLyricsExtractor
import com.keiranhaas.auralarc.storage.EmbeddedLyricsResult
import com.keiranhaas.auralarc.storage.LrcLyricsFinder
import com.keiranhaas.auralarc.storage.LyricsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.SystemClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object LyricsState {

    val lyricsCache =
        mutableStateMapOf<String, EmbeddedLyricsResult?>()

    val cacheRevision =
        mutableStateOf(
            0
        )

    /*
     * A failed lyrics lookup should not remain a permanent failure.
     *
     * This is especially important for:
     * - temporarily unavailable SAF storage
     * - temporary Navidrome network failures
     * - lyrics added while the app remains running
     */
    private const val FAILED_LOOKUP_RETRY_MS =
        60_000L

    private val completedAttempts =
        ConcurrentHashMap.newKeySet<String>()

    private val retryAfterElapsedTime =
        ConcurrentHashMap<String, Long>()

    private val inFlightLoads =
        ConcurrentHashMap<
                String,
                CompletableDeferred<EmbeddedLyricsResult?>
                >()

    suspend fun preloadLyrics(
        context: Context,
        track: MusicTrack
    ) {
        val uri =
            track.uri

        /*
         * Positive results don't need another scan.
         */
        if (
            lyricsCache[
                uri
            ] != null
        ) {
            return
        }

        val now =
            SystemClock.elapsedRealtime()

        val retryAfter =
            retryAfterElapsedTime[
                uri
            ] ?: 0L

        if (
            retryAfter > now
        ) {
            return
        }

        /*
         * CompletableDeferred replaces the previous 10 ms polling loop.
         *
         * If two parts of the app request the same lyrics simultaneously,
         * the second simply awaits the first.
         */
        val ourLoad =
            CompletableDeferred<EmbeddedLyricsResult?>()

        val existingLoad =
            inFlightLoads.putIfAbsent(
                uri,
                ourLoad
            )

        if (
            existingLoad != null
        ) {
            existingLoad.await()

            return
        }

        try {
            val result =
                withContext(
                    Dispatchers.IO
                ) {
                    try {
                        loadLyricsInternal(
                            context =
                                context.applicationContext,
                            track = track
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }

            lyricsCache[
                uri
            ] =
                result

            completedAttempts.add(
                uri
            )

            if (
                result == null
            ) {
                retryAfterElapsedTime[
                    uri
                ] =
                    SystemClock.elapsedRealtime() +
                            FAILED_LOOKUP_RETRY_MS
            } else {
                retryAfterElapsedTime.remove(
                    uri
                )
            }

            cacheRevision.value +=
                1

            ourLoad.complete(
                result
            )
        } catch (
            throwable: Throwable
        ) {
            completedAttempts.add(
                uri
            )

            retryAfterElapsedTime[
                uri
            ] =
                SystemClock.elapsedRealtime() +
                        FAILED_LOOKUP_RETRY_MS

            ourLoad.complete(
                null
            )
        } finally {
            inFlightLoads.remove(
                uri,
                ourLoad
            )
        }
    }

    fun getLyrics(
        track: MusicTrack
    ): EmbeddedLyricsResult? {
        return lyricsCache[
            track.uri
        ]
    }

    fun hasFinishedLoading(
        track: MusicTrack
    ): Boolean {
        return completedAttempts.contains(
            track.uri
        ) &&
                !inFlightLoads.containsKey(
                    track.uri
                )
    }

    fun clearCache() {
        lyricsCache.clear()

        completedAttempts.clear()

        retryAfterElapsedTime.clear()

        inFlightLoads
            .values
            .forEach { load ->
                load.cancel()
            }

        inFlightLoads.clear()

        LrcLyricsFinder.invalidateIndex()

        cacheRevision.value +=
            1
    }

    fun isNavidromeTrack(
        trackUri: String
    ): Boolean {
        return getNavidromeSongIdFromStreamUrl(
            trackUri
        ) != null
    }

    private fun loadLyricsInternal(
        context: Context,
        track: MusicTrack
    ): EmbeddedLyricsResult? {
        if (
            LyricsPreferences.getDuetLyricsEnabled(
                context
            )
        ) {
            val duetLyrics =
                LrcLyricsFinder.findDuetLyricsForTrack(
                    context = context,
                    track = track
                )

            if (
                duetLyrics != null
            ) {
                return duetLyrics
            }
        }

        val externalLrc =
            LrcLyricsFinder.findLyricsForTrack(
                context = context,
                track = track
            )

        if (
            externalLrc != null
        ) {
            return externalLrc
        }

        val songId =
            getNavidromeSongIdFromStreamUrl(
                track.uri
            )

        if (
            songId != null
        ) {
            val credentials =
                NavidromePreferences.getCredentials(
                    context
                ) ?: return null

            return try {
                NavidromeClient(
                    credentials
                ).getLyricsForSongId(
                    songId
                )
            } catch (_: Throwable) {
                null
            }
        }

        return EmbeddedLyricsExtractor.getEmbeddedLyrics(
            context = context,
            trackUri = track.uri
        )
    }

    private fun getNavidromeSongIdFromStreamUrl(
        trackUri: String
    ): String? {
        return try {
            val uri =
                Uri.parse(
                    trackUri
                )

            val path =
                uri.path ?: ""

            val looksLikeNavidromeStream =
                path.contains(
                    "stream.view"
                ) ||
                        path.contains(
                            "download.view"
                        )

            if (
                !looksLikeNavidromeStream
            ) {
                return null
            }

            uri.getQueryParameter(
                "id"
            )?.takeIf {
                it.isNotBlank()
            }
        } catch (_: Throwable) {
            null
        }
    }
}