package com.keiranhaas.auralarc.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object PlaylistTransferManager {

    data class ImportResult(
        val playlist: Playlist?,
        val importedTrackCount: Int,
        val missingEntries: List<String>
    )

    private data class ExtendedInfo(
        val artist: String,
        val title: String
    )

    fun importM3u(
        context: Context,
        uri: Uri,
        allTracks: List<MusicTrack>
    ): ImportResult {
        val lines =
            context.contentResolver
                .openInputStream(
                    uri
                )
                ?.use { inputStream ->
                    BufferedReader(
                        InputStreamReader(
                            inputStream,
                            Charsets.UTF_8
                        )
                    ).readLines()
                }
                ?: return ImportResult(
                    playlist = null,
                    importedTrackCount = 0,
                    missingEntries = emptyList()
                )

        val resolvedTracks =
            mutableListOf<MusicTrack>()

        val missingEntries =
            mutableListOf<String>()

        var pendingInfo: ExtendedInfo? =
            null

        lines.forEach { rawLine ->
            val line =
                rawLine.trim()

            when {
                line.isBlank() -> {
                    Unit
                }

                line.startsWith(
                    "#EXTINF:",
                    ignoreCase = true
                ) -> {
                    pendingInfo =
                        parseExtendedInfo(
                            line
                        )
                }

                line.startsWith(
                    "#"
                ) -> {
                    Unit
                }

                else -> {
                    val resolvedTrack =
                        findMatchingTrack(
                            pathOrUri = line,
                            extendedInfo = pendingInfo,
                            allTracks = allTracks
                        )

                    if (
                        resolvedTrack != null
                    ) {
                        resolvedTracks.add(
                            resolvedTrack
                        )
                    } else {
                        missingEntries.add(
                            pendingInfo
                                ?.let { info ->
                                    if (
                                        info.artist.isNotBlank()
                                    ) {
                                        "${info.artist} - ${info.title}"
                                    } else {
                                        info.title
                                    }
                                }
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: line
                        )
                    }

                    pendingInfo =
                        null
                }
            }
        }

        if (
            resolvedTracks.isEmpty()
        ) {
            return ImportResult(
                playlist = null,
                importedTrackCount = 0,
                missingEntries = missingEntries
            )
        }

        val playlistName =
            getDocumentDisplayName(
                context = context,
                uri = uri
            )
                .replace(
                    Regex(
                        "(?i)\\.m3u8?$"
                    ),
                    ""
                )
                .trim()
                .ifBlank {
                    "Imported Playlist"
                }

        val playlist =
            PlaylistStore.createPlaylistFromTracks(
                context = context,
                rawName = playlistName,
                tracks = resolvedTracks
            )

        return ImportResult(
            playlist = playlist,
            importedTrackCount = resolvedTracks.size,
            missingEntries = missingEntries
        )
    }

    fun exportM3u(
        context: Context,
        uri: Uri,
        playlist: Playlist,
        allTracks: List<MusicTrack>
    ): Int {
        val tracks =
            PlaylistStore.getTracksForPlaylist(
                playlist = playlist,
                allTracks = allTracks
            )

        context.contentResolver
            .openOutputStream(
                uri,
                "wt"
            )
            ?.use { outputStream ->
                OutputStreamWriter(
                    outputStream,
                    Charsets.UTF_8
                ).buffered().use { writer ->
                    writer.appendLine(
                        "#EXTM3U"
                    )

                    tracks.forEach { track ->
                        val durationSeconds =
                            if (
                                track.duration > 0L
                            ) {
                                track.duration / 1000L
                            } else {
                                -1L
                            }

                        val artist =
                            cleanM3uText(
                                track.artist
                            )

                        val title =
                            cleanM3uText(
                                track.title
                            )

                        writer.appendLine(
                            "#EXTINF:$durationSeconds,$artist - $title"
                        )

                        writer.appendLine(
                            track.sourcePath
                                .trim()
                                .ifBlank {
                                    track.uri
                                }
                        )
                    }
                }
            }
            ?: return 0

        return tracks.size
    }

    private fun parseExtendedInfo(
        line: String
    ): ExtendedInfo? {
        val displayText =
            line.substringAfter(
                ',',
                ""
            ).trim()

        if (
            displayText.isBlank()
        ) {
            return null
        }

        val separatorIndex =
            displayText.indexOf(
                " - "
            )

        return if (
            separatorIndex >= 0
        ) {
            ExtendedInfo(
                artist = displayText
                    .substring(
                        0,
                        separatorIndex
                    )
                    .trim(),
                title = displayText
                    .substring(
                        separatorIndex + 3
                    )
                    .trim()
            )
        } else {
            ExtendedInfo(
                artist = "",
                title = displayText
            )
        }
    }

    private fun findMatchingTrack(
        pathOrUri: String,
        extendedInfo: ExtendedInfo?,
        allTracks: List<MusicTrack>
    ): MusicTrack? {
        val normalizedEntry =
            normalizeLocation(
                pathOrUri
            )

        allTracks.firstOrNull { track ->
            normalizeLocation(
                track.uri
            ) == normalizedEntry ||
                    (
                            track.sourcePath.isNotBlank() &&
                                    normalizeLocation(
                                        track.sourcePath
                                    ) == normalizedEntry
                            )
        }?.let {
            return it
        }

        if (
            extendedInfo != null &&
            extendedInfo.title.isNotBlank()
        ) {
            allTracks.firstOrNull { track ->
                track.title.trim().equals(
                    extendedInfo.title.trim(),
                    ignoreCase = true
                ) &&
                        (
                                extendedInfo.artist.isBlank() ||
                                        track.artist.trim().equals(
                                            extendedInfo.artist.trim(),
                                            ignoreCase = true
                                        ) ||
                                        track.albumArtist.trim().equals(
                                            extendedInfo.artist.trim(),
                                            ignoreCase = true
                                        )
                                )
            }?.let {
                return it
            }
        }

        val entryFileName =
            normalizedEntry
                .substringAfterLast(
                    '/'
                )

        if (
            entryFileName.isBlank()
        ) {
            return null
        }

        return allTracks.firstOrNull { track ->
            val sourceFileName =
                normalizeLocation(
                    track.sourcePath
                        .ifBlank {
                            track.uri
                        }
                )
                    .substringAfterLast(
                        '/'
                    )

            sourceFileName.equals(
                entryFileName,
                ignoreCase = true
            )
        }
    }

    private fun normalizeLocation(
        value: String
    ): String {
        return Uri.decode(
            value.trim()
        )
            .replace(
                '\\',
                '/'
            )
            .removePrefix(
                "file://"
            )
            .trim()
            .lowercase()
    }

    private fun cleanM3uText(
        value: String
    ): String {
        return value
            .replace(
                '\n',
                ' '
            )
            .replace(
                '\r',
                ' '
            )
            .trim()
    }

    private fun getDocumentDisplayName(
        context: Context,
        uri: Uri
    ): String {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (
                    cursor.moveToFirst()
                ) {
                    val index =
                        cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                        )

                    if (
                        index >= 0
                    ) {
                        cursor.getString(
                            index
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
            ?: uri.lastPathSegment
            ?: "Imported Playlist"
    }
}