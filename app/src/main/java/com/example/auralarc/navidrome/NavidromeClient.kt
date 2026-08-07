package com.example.auralarc.navidrome

import android.net.Uri
import android.util.Log
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.data.Playlist
import com.example.auralarc.storage.EmbeddedLyricsResult
import com.example.auralarc.storage.EmbeddedLyricsType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import com.example.auralarc.utils.codecLabelFromMimeOrName
import com.example.auralarc.utils.inferLossless

class NavidromeClient(
    private val credentials: NavidromeCredentials
) {
    private val apiVersion =
        "1.16.1"

    private val clientName =
        "AuralArc"

    private val sessionSalt =
        NavidromeAuth.createSalt()

    private val sessionToken =
        NavidromeAuth.createToken(
            password = credentials.password,
            salt = sessionSalt
        )

    private fun formatLrcTimestamp(
        milliseconds: Long
    ): String {
        val totalSeconds =
            milliseconds / 1000L

        val minutes =
            totalSeconds / 60L

        val seconds =
            totalSeconds % 60L

        val hundredths =
            (milliseconds % 1000L) / 10L

        return "[${minutes}:${seconds.toString().padStart(2, '0')}.${hundredths.toString().padStart(2, '0')}]"
    }

    fun ping(): NavidromeApiResult {
        return try {
            val response =
                getJson(
                    endpoint = "ping.view"
                )

            val status =
                response.optString(
                    "status",
                    "failed"
                )

            if (
                status == "ok"
            ) {
                NavidromeApiResult(
                    success = true,
                    message = "Connected to Navidrome successfully.",
                    rawJson = response.toString(
                        2
                    )
                )
            } else {
                NavidromeApiResult(
                    success = false,
                    message = extractErrorMessage(
                        response
                    ),
                    rawJson = response.toString(
                        2
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(
                "AuralArc",
                "Navidrome ping failed",
                e
            )

            NavidromeApiResult(
                success = false,
                message = e.message ?: "Could not connect to Navidrome."
            )
        }
    }

    fun getAllSongs(): List<MusicTrack> {
        val tracks =
            mutableListOf<MusicTrack>()

        val pageSize =
            500

        var offset =
            0

        while (true) {
            val albums =
                getAlbumListPage(
                    size = pageSize,
                    offset = offset
                )

            if (
                albums.length() == 0
            ) {
                break
            }

            for (
            index in 0 until albums.length()
            ) {
                val albumObject =
                    albums.optJSONObject(
                        index
                    ) ?: continue

                val albumId =
                    albumObject.optString(
                        "id",
                        ""
                    )

                if (
                    albumId.isBlank()
                ) {
                    continue
                }

                try {
                    val albumSongs =
                        getSongsFromAlbum(
                            albumId
                        )

                    for (
                    songIndex in 0 until albumSongs.length()
                    ) {
                        val songObject =
                            albumSongs.optJSONObject(
                                songIndex
                            ) ?: continue

                        val track =
                            songJsonToMusicTrack(
                                songObject
                            )

                        if (
                            track != null
                        ) {
                            tracks.add(
                                track
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "AuralArc",
                        "Failed loading Navidrome album $albumId",
                        e
                    )
                }
            }

            if (
                albums.length() < pageSize
            ) {
                break
            }

            offset +=
                pageSize
        }

        return tracks
            .distinctBy {
                it.uri
            }
            .sortedWith(
                compareBy<MusicTrack> {
                    it.artist.lowercase()
                }.thenBy {
                    it.album.lowercase()
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

    fun getRemotePlaylists(): List<Playlist> {
        val response =
            getJson(
                endpoint = "getPlaylists.view"
            )

        val playlistsObject =
            response.optJSONObject(
                "playlists"
            )

        val playlistArray =
            jsonArrayOrSingleObject(
                parent = playlistsObject,
                key = "playlist"
            )

        val playlists =
            mutableListOf<Playlist>()

        for (
        index in 0 until playlistArray.length()
        ) {
            val playlistObject =
                playlistArray.optJSONObject(
                    index
                ) ?: continue

            val playlistId =
                playlistObject.optString(
                    "id",
                    ""
                )

            if (
                playlistId.isBlank()
            ) {
                continue
            }

            val name =
                cleanText(
                    playlistObject.optString(
                        "name",
                        ""
                    ),
                    "Navidrome Playlist"
                )

            val trackCount =
                playlistObject.optInt(
                    "songCount",
                    0
                )

            playlists.add(
                Playlist(
                    id = playlistId,
                    name = name,
                    source = "NAVIDROME",
                    remoteId = playlistId,
                    trackKeys = emptyList(),
                    trackCount = trackCount
                )
            )
        }

        return playlists.sortedBy {
            it.name.lowercase()
        }
    }

    fun getRemotePlaylistDetail(
        playlistId: String
    ): Pair<Playlist, List<MusicTrack>>? {
        val response =
            getJson(
                endpoint = "getPlaylist.view",
                extraParams = mapOf(
                    "id" to playlistId
                )
            )

        val playlistObject =
            response.optJSONObject(
                "playlist"
            ) ?: return null

        val playlistName =
            cleanText(
                playlistObject.optString(
                    "name",
                    ""
                ),
                "Navidrome Playlist"
            )

        val songArray =
            jsonArrayOrSingleObject(
                parent = playlistObject,
                key = "entry"
            )

        val tracks =
            mutableListOf<MusicTrack>()

        for (
        index in 0 until songArray.length()
        ) {
            val songObject =
                songArray.optJSONObject(
                    index
                ) ?: continue

            val track =
                songJsonToMusicTrack(
                    songObject
                )

            if (
                track != null
            ) {
                tracks.add(
                    track
                )
            }
        }

        val playlist =
            Playlist(
                id = playlistId,
                name = playlistName,
                source = "NAVIDROME",
                remoteId = playlistId,
                trackKeys = emptyList(),
                trackCount = tracks.size
            )

        return Pair(
            playlist,
            tracks
        )
    }

    fun getLyricsForSongId(
        songId: String
    ): EmbeddedLyricsResult? {
        val response =
            getJson(
                endpoint = "getLyricsBySongId.view",
                extraParams = mapOf(
                    "id" to songId
                )
            )

        val lyricsList =
            response.optJSONObject(
                "lyricsList"
            ) ?: return null

        val structuredLyricsArray =
            jsonArrayOrSingleObject(
                parent = lyricsList,
                key = "structuredLyrics"
            )

        if (
            structuredLyricsArray.length() == 0
        ) {
            return null
        }

        var fallbackUnsynced: EmbeddedLyricsResult? =
            null

        for (
        index in 0 until structuredLyricsArray.length()
        ) {
            val lyricsObject =
                structuredLyricsArray.optJSONObject(
                    index
                ) ?: continue

            val synced =
                lyricsObject.optBoolean(
                    "synced",
                    false
                )

            val lineArray =
                jsonArrayOrSingleObject(
                    parent = lyricsObject,
                    key = "line"
                )

            val lines =
                mutableListOf<String>()

            for (
            lineIndex in 0 until lineArray.length()
            ) {
                val lineObject =
                    lineArray.optJSONObject(
                        lineIndex
                    ) ?: continue

                val value =
                    lineObject.optString(
                        "value",
                        ""
                    ).trim()

                if (
                    value.isBlank()
                ) {
                    continue
                }

                if (
                    synced
                ) {
                    val start =
                        lineObject.optLong(
                            "start",
                            -1L
                        )

                    if (
                        start >= 0L
                    ) {
                        lines.add(
                            "${formatLrcTimestamp(start)}$value"
                        )
                    } else {
                        lines.add(
                            value
                        )
                    }
                } else {
                    lines.add(
                        value
                    )
                }
            }

            val text =
                lines.joinToString(
                    "\n"
                ).trim()

            if (
                text.isBlank()
            ) {
                continue
            }

            val result =
                EmbeddedLyricsResult(
                    text = text,
                    type =
                    if (
                        synced
                    ) {
                        EmbeddedLyricsType.SYNCED
                    } else {
                        EmbeddedLyricsType.UNSYNCED
                    },
                    source = "Navidrome"
                )

            if (
                synced
            ) {
                return result
            }

            fallbackUnsynced =
                result
        }

        return fallbackUnsynced
    }

    private fun getAlbumListPage(
        size: Int,
        offset: Int
    ): JSONArray {
        val response =
            getJson(
                endpoint = "getAlbumList2.view",
                extraParams = mapOf(
                    "type" to "alphabeticalByName",
                    "size" to size.toString(),
                    "offset" to offset.toString()
                )
            )

        return jsonArrayOrSingleObject(
            parent = response.optJSONObject(
                "albumList2"
            ),
            key = "album"
        )
    }

    private fun getSongsFromAlbum(
        albumId: String
    ): JSONArray {
        val response =
            getJson(
                endpoint = "getAlbum.view",
                extraParams = mapOf(
                    "id" to albumId
                )
            )

        return jsonArrayOrSingleObject(
            parent = response.optJSONObject(
                "album"
            ),
            key = "song"
        )
    }

    fun getJson(
        endpoint: String,
        extraParams: Map<String, String> = emptyMap()
    ): JSONObject {
        val requestUrl =
            buildApiUrl(
                endpoint = endpoint,
                extraParams = extraParams
            )

        Log.d(
            "AuralArc",
            "Navidrome GET endpoint: $endpoint"
        )

        val connection =
            URL(
                requestUrl
            ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15000

            connection.readTimeout =
                30000

            val responseCode =
                connection.responseCode

            val inputStream =
                if (
                    responseCode in 200..299
                ) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val responseText =
                BufferedReader(
                    InputStreamReader(
                        inputStream
                    )
                ).use { reader ->
                    reader.readText()
                }

            if (
                responseCode !in 200..299
            ) {
                throw IllegalStateException(
                    "HTTP $responseCode: $responseText"
                )
            }

            val root =
                JSONObject(
                    responseText
                )

            val subsonicResponse =
                root.optJSONObject(
                    "subsonic-response"
                ) ?: throw IllegalStateException(
                    "Invalid Navidrome response."
                )

            val status =
                subsonicResponse.optString(
                    "status",
                    "failed"
                )

            if (
                status != "ok"
            ) {
                throw IllegalStateException(
                    extractErrorMessage(
                        subsonicResponse
                    )
                )
            }

            return subsonicResponse
        } finally {
            connection.disconnect()
        }
    }

    fun buildStreamUrl(
        songId: String
    ): String {
        return buildApiUrl(
            endpoint = "stream.view",
            extraParams = mapOf(
                "id" to songId,
                "format" to "raw"
            )
        )
    }

    fun buildCoverArtUrl(
        coverArtId: String
    ): String {
        return buildApiUrl(
            endpoint = "getCoverArt.view",
            extraParams = mapOf(
                "id" to coverArtId,
                "size" to "300"
            )
        )
    }

    private fun buildApiUrl(
        endpoint: String,
        extraParams: Map<String, String> = emptyMap()
    ): String {
        val cleanBaseUrl =
            credentials.serverUrl
                .trim()
                .trimEnd('/')

        val builder =
            Uri.parse(
                "$cleanBaseUrl/rest/$endpoint"
            )
                .buildUpon()
                .appendQueryParameter(
                    "u",
                    credentials.username
                )
                .appendQueryParameter(
                    "t",
                    sessionToken
                )
                .appendQueryParameter(
                    "s",
                    sessionSalt
                )
                .appendQueryParameter(
                    "v",
                    apiVersion
                )
                .appendQueryParameter(
                    "c",
                    clientName
                )
                .appendQueryParameter(
                    "f",
                    "json"
                )

        extraParams.forEach { entry ->
            builder.appendQueryParameter(
                entry.key,
                entry.value
            )
        }

        return builder
            .build()
            .toString()
    }

    private fun songJsonToMusicTrack(
        songObject: JSONObject
    ): MusicTrack? {
        val songId =
            songObject.optString(
                "id",
                ""
            )

        if (
            songId.isBlank()
        ) {
            return null
        }

        val title =
            cleanText(
                songObject.optString(
                    "title",
                    ""
                ),
                "Unknown Title"
            )

        val artist =
            cleanText(
                songObject.optString(
                    "artist",
                    ""
                ),
                "Unknown Artist"
            )

        val albumArtist =
            readNavidromeAlbumArtist(
                songObject = songObject,
                fallbackArtist = artist
            )

        val album =
            cleanText(
                songObject.optString(
                    "album",
                    ""
                ),
                "Unknown Album"
            )

        val genre =
            songObject.optString(
                "genre",
                ""
            ).trim()

        val albumIdString =
            cleanText(
                songObject.optString(
                    "albumId",
                    ""
                ),
                songObject.optString(
                    "parent",
                    songId
                )
            )

        val coverArtId =
            songObject.optString(
                "coverArt",
                ""
            )

        val durationMillis =
            songObject.optLong(
                "duration",
                0L
            ) * 1000L

        val trackNumber =
            songObject.optInt(
                "track",
                0
            )

        val releaseYear =
            songObject.optInt(
                "year",
                0
            ).coerceAtLeast(
                0
            )

        val releaseDate =
            if (
                releaseYear > 0
            ) {
                releaseYear.toString()
            } else {
                ""
            }

        val suffix =
            songObject.optString(
                "suffix",
                ""
            )

        val contentType =
            songObject.optString(
                "contentType",
                ""
            )

        val bitRateKbps =
            songObject.optInt(
                "bitRate",
                0
            )

        val bitrate =
            if (
                bitRateKbps > 0
            ) {
                bitRateKbps * 1000
            } else {
                0
            }

        val sampleRate =
            songObject.optInt(
                "samplingRate",
                songObject.optInt(
                    "sampleRate",
                    0
                )
            )

        val bitDepth =
            songObject.optInt(
                "bitDepth",
                songObject.optInt(
                    "bitsPerSample",
                    0
                )
            )

        val channelCount =
            songObject.optInt(
                "channelCount",
                0
            )

        val audioCodec =
            codecLabelFromMimeOrName(
                mimeType = contentType,
                nameOrUri = suffix
            )

        val fileSizeBytes =
            songObject.optLong(
                "size",
                0L
            )

        val streamUrl =
            buildStreamUrl(
                songId
            )

        val coverArtUrl =
            if (
                coverArtId.isNotBlank()
            ) {
                buildCoverArtUrl(
                    coverArtId
                )
            } else {
                null
            }

        val sourcePath =
            songObject.optString(
                "path",
                streamUrl
            )

        val discNumber =
            songObject.optInt(
                "discNumber",
                0
            )

        val replayGainObject =
            songObject.optJSONObject(
                "replayGain"
            )

        val replayGainTrackGain =
            replayGainObject?.optDouble(
                "trackGain",
                Double.NaN
            )?.takeIf {
                !it.isNaN()
            }?.toFloat()

        val replayGainAlbumGain =
            replayGainObject?.optDouble(
                "albumGain",
                Double.NaN
            )?.takeIf {
                !it.isNaN()
            }?.toFloat()

        val replayGainTrackPeak =
            replayGainObject?.optDouble(
                "trackPeak",
                Double.NaN
            )?.takeIf {
                !it.isNaN()
            }?.toFloat()

        val replayGainAlbumPeak =
            replayGainObject?.optDouble(
                "albumPeak",
                Double.NaN
            )?.takeIf {
                !it.isNaN()
            }?.toFloat()

        return MusicTrack(
            id = stableLongId(
                songId
            ),
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            duration = durationMillis,
            uri = streamUrl,
            albumId = stableLongId(
                albumIdString
            ),
            trackNumber = trackNumber,
            albumArtPath = coverArtUrl,
            releaseDate = releaseDate,
            releaseYear = releaseYear,
            audioCodec = audioCodec,
            mimeType = contentType,
            bitrate = bitrate,
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            channelCount = channelCount,
            fileSizeBytes = fileSizeBytes,
            discNumber = discNumber,
            container = suffix.uppercase(),
            fileExtension = suffix,
            sourceType = "Navidrome Stream",
            sourcePath = sourcePath,
            lossless = inferLossless(
                mimeType = contentType,
                nameOrUri = suffix
            ),
            replayGainTrackGain = replayGainTrackGain,
            replayGainAlbumGain = replayGainAlbumGain,
            replayGainTrackPeak = replayGainTrackPeak,
            replayGainAlbumPeak = replayGainAlbumPeak,
            metadataWarning = "Remote streams expose less low-level extractor data than local files."
        )
    }

    private fun jsonArrayOrSingleObject(
        parent: JSONObject?,
        key: String
    ): JSONArray {
        val array =
            parent?.optJSONArray(
                key
            )

        if (
            array != null
        ) {
            return array
        }

        val singleObject =
            parent?.optJSONObject(
                key
            )

        val fallbackArray =
            JSONArray()

        if (
            singleObject != null
        ) {
            fallbackArray.put(
                singleObject
            )
        }

        return fallbackArray
    }

    private fun readNavidromeAlbumArtist(
        songObject: JSONObject,
        fallbackArtist: String
    ): String {
        val displayAlbumArtist =
            songObject.optString(
                "displayAlbumArtist",
                ""
            ).trim()

        if (
            displayAlbumArtist.isNotBlank() &&
            !displayAlbumArtist.equals(
                "<unknown>",
                ignoreCase = true
            )
        ) {
            return displayAlbumArtist
        }

        val legacyAlbumArtist =
            songObject.optString(
                "albumArtist",
                ""
            ).trim()

        if (
            legacyAlbumArtist.isNotBlank() &&
            !legacyAlbumArtist.equals(
                "<unknown>",
                ignoreCase = true
            )
        ) {
            return legacyAlbumArtist
        }

        val albumArtistsArray =
            songObject.optJSONArray(
                "albumArtists"
            )

        if (
            albumArtistsArray != null
        ) {
            val albumArtistNames =
                mutableListOf<String>()

            for (
            index in 0 until albumArtistsArray.length()
            ) {
                val rawValue =
                    albumArtistsArray.opt(
                        index
                    )

                val artistName =
                    when (
                        rawValue
                    ) {
                        is JSONObject ->
                            rawValue.optString(
                                "name",
                                ""
                            )

                        is String ->
                            rawValue

                        else ->
                            ""
                    }.trim()

                if (
                    artistName.isNotBlank() &&
                    !artistName.equals(
                        "<unknown>",
                        ignoreCase = true
                    )
                ) {
                    albumArtistNames.add(
                        artistName
                    )
                }
            }

            if (
                albumArtistNames.isNotEmpty()
            ) {
                return albumArtistNames
                    .distinct()
                    .joinToString(
                        "; "
                    )
            }
        }

        /*
         * Some servers only return the legacy song-artist string.
         * Navidrome commonly separates multiple performers using
         * semicolons, so the first performer is the safest final
         * fallback for album categorization.
         */
        return fallbackArtist
            .substringBefore(
                ";"
            )
            .trim()
            .ifBlank {
                "Unknown Artist"
            }
    }

    private fun cleanText(
        value: String?,
        fallback: String
    ): String {
        return value
            ?.takeIf {
                it.isNotBlank() &&
                        it != "<unknown>"
            }
            ?: fallback
    }

    private fun stableLongId(
        value: String
    ): Long {
        val bytes =
            MessageDigest
                .getInstance(
                    "MD5"
                )
                .digest(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )

        var result =
            0L

        for (
        index in 0 until 8
        ) {
            result =
                result shl 8

            result =
                result or (
                        bytes[index].toLong() and 0xffL
                        )
        }

        return result
    }

    private fun extractErrorMessage(
        response: JSONObject
    ): String {
        return response
            .optJSONObject(
                "error"
            )
            ?.optString(
                "message"
            )
            ?: "Navidrome rejected the request."
    }
}