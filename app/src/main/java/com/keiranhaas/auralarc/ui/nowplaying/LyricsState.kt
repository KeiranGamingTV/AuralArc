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

object LyricsState {

    val lyricsCache =
        mutableStateMapOf<String, EmbeddedLyricsResult?>()

    val cacheRevision =
        mutableStateOf(
            0
        )

    private val loadedUris =
        mutableSetOf<String>()

    private val loadingUris =
        mutableSetOf<String>()

    suspend fun preloadLyrics(
        context: Context,
        track: MusicTrack
    ) {
        val uri =
            track.uri

        val shouldLoad =
            synchronized(
                loadingUris
            ) {
                if (
                    loadedUris.contains(
                        uri
                    ) ||
                    loadingUris.contains(
                        uri
                    )
                ) {
                    false
                } else {
                    loadingUris.add(
                        uri
                    )

                    true
                }
            }

        if (
            !shouldLoad
        ) {
            /*
             * Another request is already loading this track.
             * Wait briefly for that request rather than starting a
             * second full filesystem search.
             */
            while (
                synchronized(
                    loadingUris
                ) {
                    loadingUris.contains(
                        uri
                    )
                }
            ) {
                kotlinx.coroutines.delay(
                    10L
                )
            }

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
                            track =
                                track
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }

            lyricsCache[uri] =
                result

            loadedUris.add(
                uri
            )
        } finally {
            synchronized(
                loadingUris
            ) {
                loadingUris.remove(
                    uri
                )
            }
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
        return loadedUris.contains(
            track.uri
        )
    }

    fun clearCache() {
        lyricsCache.clear()
        loadedUris.clear()

        synchronized(
            loadingUris
        ) {
            loadingUris.clear()
        }

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