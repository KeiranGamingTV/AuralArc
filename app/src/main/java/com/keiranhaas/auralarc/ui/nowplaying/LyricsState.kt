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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object LyricsState {

    val lyricsCache =
        mutableStateMapOf<String, EmbeddedLyricsResult?>()

    val cacheRevision =
        mutableStateOf(
            0
        )

    private val completedAttempts =
        ConcurrentHashMap.newKeySet<String>()

    private val inFlightLoads =
        ConcurrentHashMap<
                String,
                CompletableDeferred<EmbeddedLyricsResult?>
                >()

    /*
     * A null result can happen because the track's metadata, MediaStore,
     * or SAF storage is not ready yet when Now Playing first opens.
     *
     * Do not retry continuously, but also do not treat a null result
     * as a permanent "no lyrics" state.
     */
    private val retryAfterElapsedTime =
        ConcurrentHashMap<String, Long>()

    suspend fun preloadLyrics(
        context: Context,
        track: MusicTrack,
        forceReload: Boolean = false
    ) {
        val uri =
            track.uri

        if (
            uri.isBlank()
        ) {
            return
        }

        /*
         * Reuse an already successful result.
         *
         * A cached null is different: it may have been caused by a
         * temporary lookup race, so allow another attempt after the
         * short retry delay.
         */
        if (
            !forceReload &&
            lyricsCache.containsKey(
                uri
            ) &&
            !inFlightLoads.containsKey(
                uri
            )
        ) {
            val cachedResult =
                lyricsCache[uri]

            if (
                cachedResult != null
            ) {
                completedAttempts.add(
                    uri
                )

                return
            }
        }

        /*
         * Never perform multiple simultaneous searches for the same track.
         */
        val existingLoad =
            inFlightLoads[
                uri
            ]

        if (
            existingLoad != null
        ) {
            existingLoad.await()
            return
        }

        val deferred =
            CompletableDeferred<EmbeddedLyricsResult?>()

        val competingLoad =
            inFlightLoads.putIfAbsent(
                uri,
                deferred
            )

        if (
            competingLoad != null
        ) {
            competingLoad.await()
            return
        }

        try {
            /*
             * Try several times before accepting that lyrics are unavailable.
             *
             * This specifically prevents the first Now Playing composition
             * from permanently caching "No lyrics were found" when the
             * library/SAF metadata becomes available a moment later.
             */
            var result:
                    EmbeddedLyricsResult? =
                null

            val retryDelays =
                longArrayOf(
                    0L,
                    500L,
                    1_000L,
                    2_000L,
                    3_000L
                )

            for (
            delayMs in retryDelays
            ) {
                if (
                    delayMs > 0L
                ) {
                    kotlinx.coroutines.delay(
                        delayMs
                    )
                }

                result =
                    withContext(
                        Dispatchers.IO
                    ) {
                        try {
                            loadLyricsInternal(
                                context =
                                    context.applicationContext,
                                track =
                                    track
                            )
                        } catch (
                            _: Throwable
                        ) {
                            null
                        }
                    }

                if (
                    result != null
                ) {
                    break
                }
            }

            lyricsCache[
                uri
            ] =
                result

            completedAttempts.add(
                uri
            )

            retryAfterElapsedTime.remove(
                uri
            )

            cacheRevision.value +=
                1

            deferred.complete(
                result
            )
        } catch (
            _: Throwable
        ) {
            lyricsCache[
                uri
            ] =
                null

            completedAttempts.add(
                uri
            )

            retryAfterElapsedTime.remove(
                uri
            )

            cacheRevision.value +=
                1

            deferred.complete(
                null
            )
        } finally {
            inFlightLoads.remove(
                uri,
                deferred
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

        cacheRevision.value +=
            1
    }

    fun invalidateLyricsFileIndex() {
        LrcLyricsFinder.invalidateIndex()

        clearCache()
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

        val duetLyricsEnabled =
            LyricsPreferences.getDuetLyricsEnabled(
                context
            )

        /*
         * 1. When Duet Lyrics is enabled, exhaust BOTH .dlrc lookup paths
         * before looking for a normal .lrc.
         *
         * This is important when both:
         *
         *     Song.lrc
         *     Song.dlrc
         *
         * exist beside the same audio file.
         *
         * The .dlrc must win whenever Duet Lyrics is enabled.
         */
        if (
            duetLyricsEnabled
        ) {
            val quickDuetLyrics =
                LrcLyricsFinder.findDuetLyricsForTrackQuick(
                    context = context,
                    track = track
                )

            if (
                quickDuetLyrics != null
            ) {
                return quickDuetLyrics
            }

            val indexedDuetLyrics =
                LrcLyricsFinder.findDuetLyricsForTrackIndexed(
                    context = context,
                    track = track
                )

            if (
                indexedDuetLyrics != null
            ) {
                return indexedDuetLyrics
            }
        }

        /*
         * 2. Only look for a normal .lrc after all possible .dlrc
         * lookups have failed.
         *
         * Duet enabled + .dlrc found
         *             -> .dlrc / DUET_SYNCED
         *
         * Duet enabled + no .dlrc
         *             -> normal .lrc
         *
         * Duet disabled
         *             -> normal .lrc
         */
        val quickExternalLyrics =
            LrcLyricsFinder.findLyricsForTrackQuick(
                context = context,
                track = track
            )

        if (
            quickExternalLyrics != null
        ) {
            return quickExternalLyrics
        }

        /*
         * 3. Navidrome lyrics.
         */
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
                )

            if (
                credentials != null
            ) {
                val navidromeLyrics =
                    try {
                        NavidromeClient(
                            credentials
                        ).getLyricsForSongId(
                            songId
                        )
                    } catch (
                        _: Throwable
                    ) {
                        null
                    }

                if (
                    navidromeLyrics != null
                ) {
                    return navidromeLyrics
                }
            }
        }

        /*
         * 4. Embedded lyrics.
         */
        val embeddedLyrics =
            try {
                EmbeddedLyricsExtractor.getEmbeddedLyrics(
                    context = context,
                    trackUri = track.uri
                )
            } catch (
                _: Throwable
            ) {
                null
            }

        if (
            embeddedLyrics != null
        ) {
            return embeddedLyrics
        }

        /*
          * 5. Normal .lrc indexed/picked-folder fallback.
          *
          * Any possible .dlrc has already been checked above, so reaching
          * this point means there is no usable Duet Lyrics file.
          */
        return LrcLyricsFinder.findLyricsForTrackIndexed(
            context = context,
            track = track
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
        } catch (
            _: Throwable
        ) {
            null
        }
    }
}