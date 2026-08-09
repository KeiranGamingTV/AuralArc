package com.keiranhaas.auralarc.scanner

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.utils.extractEmbeddedAlbumArt
import com.keiranhaas.auralarc.utils.AudioMetadataReader

object MusicScanner {

    fun scan(
        context: Context
    ): List<MusicTrack> {

        val tracks =
            mutableListOf<MusicTrack>()

        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
            )

        val relativePath =
            MediaStore.MediaColumns.RELATIVE_PATH

        val selection =
            "$relativePath LIKE ? OR $relativePath LIKE ?"

        val selectionArgs =
            arrayOf(
                "Music/%",
                "AuralArc/%"
            )

        val sortOrder =
            MediaStore.Audio.Media.ALBUM + " ASC, " +
                    MediaStore.Audio.Media.TRACK + " ASC, " +
                    MediaStore.Audio.Media.TITLE + " ASC"

        val cursor =
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        cursor?.use {
            while (
                it.moveToNext()
            ) {
                try {
                    val id =
                        it.getLong(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media._ID
                            )
                        )

                    val displayName =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.DISPLAY_NAME
                            )
                        )

                    val rawTitle =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.TITLE
                            )
                        )

                    val rawArtist =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.ARTIST
                            )
                        )

                    val rawAlbumArtist =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.ALBUM_ARTIST
                            )
                        )

                    val rawAlbum =
                        it.getString(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.ALBUM
                            )
                        )

                    val duration =
                        it.getLong(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.DURATION
                            )
                        )

                    val albumId =
                        it.getLong(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.ALBUM_ID
                            )
                        )

                    val rawTrackNumber =
                        it.getInt(
                            it.getColumnIndexOrThrow(
                                MediaStore.Audio.Media.TRACK
                            )
                        )

                    val trackNumber =
                        cleanTrackNumber(
                            rawTrackNumber
                        )

                    val releaseYear =
                        try {
                            it.getInt(
                                it.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.YEAR
                                )
                            ).coerceAtLeast(
                                0
                            )
                        } catch (_: Exception) {
                            0
                        }

                    val releaseDate =
                        if (
                            releaseYear > 0
                        ) {
                            releaseYear.toString()
                        } else {
                            ""
                        }

                    val fileSizeBytes =
                        try {
                            it.getLong(
                                it.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.SIZE
                                )
                            )
                        } catch (_: Exception) {
                            0L
                        }

                    val dateAddedMillis =
                        try {
                            it.getLong(
                                it.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.DATE_ADDED
                                )
                            ) * 1000L
                        } catch (_: Exception) {
                            0L
                        }

                    val dateModifiedMillis =
                        try {
                            it.getLong(
                                it.getColumnIndexOrThrow(
                                    MediaStore.Audio.Media.DATE_MODIFIED
                                )
                            ) * 1000L
                        } catch (_: Exception) {
                            0L
                        }

                    val title =
                        rawTitle
                            ?.takeIf { value ->
                                value.isNotBlank()
                            }
                            ?: displayName
                                ?.substringBeforeLast(".")
                            ?: "Unknown Title"

                    val artist =
                        rawArtist
                            ?.takeIf { value ->
                                value.isNotBlank() &&
                                        value != "<unknown>"
                            }
                            ?: "Unknown Artist"

                    val albumArtist =
                        rawAlbumArtist
                            ?.takeIf { value ->
                                value.isNotBlank() &&
                                        value != "<unknown>"
                            }
                            ?: artist

                    val album =
                        rawAlbum
                            ?.takeIf { value ->
                                value.isNotBlank() &&
                                        value != "<unknown>"
                            }
                            ?: "Unknown Album"

                    val uri =
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                    val audioMetadata =
                        AudioMetadataReader.read(
                            context = context,
                            uri = uri,
                            fallbackName = displayName,
                            fileSizeBytes = fileSizeBytes,
                            dateModifiedMillis = dateModifiedMillis,
                            sourceType = "Local MediaStore",
                            sourcePath = displayName ?: uri.toString()
                        )

                    val albumArtPath =
                        extractEmbeddedAlbumArt(
                            context = context,
                            audioUriString = uri.toString(),
                            trackId = id
                        )

                    Log.d(
                        "AuralArc",
                        "Track art result: title=$title path=$albumArtPath"
                    )

                    tracks.add(
                        MusicTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            albumArtist = albumArtist,
                            album = album,
                            genre = audioMetadata.genre,
                            duration = duration,
                            uri = uri.toString(),
                            albumId = albumId,
                            trackNumber = trackNumber,
                            albumArtPath = albumArtPath,
                            releaseDate = releaseDate,
                            releaseYear = releaseYear,
                            audioCodec = audioMetadata.audioCodec,
                            mimeType = audioMetadata.mimeType,
                            bitrate = audioMetadata.bitrate,
                            sampleRate = audioMetadata.sampleRate,
                            bitDepth = audioMetadata.bitDepth,
                            channelCount = audioMetadata.channelCount,
                            fileSizeBytes = audioMetadata.fileSizeBytes,
                            dateAddedMillis = dateAddedMillis,
                            dateModifiedMillis = audioMetadata.dateModifiedMillis,
                            container = audioMetadata.container,
                            fileExtension = audioMetadata.fileExtension,
                            sourceType = audioMetadata.sourceType,
                            sourcePath = audioMetadata.sourcePath,
                            lossless = audioMetadata.lossless,
                            audioTrackCount = audioMetadata.audioTrackCount,
                            extractorTrackCount = audioMetadata.extractorTrackCount,
                            audioTrackIndex = audioMetadata.audioTrackIndex,
                            pcmEncoding = audioMetadata.pcmEncoding,
                            maxInputSize = audioMetadata.maxInputSize,
                            encoderDelay = audioMetadata.encoderDelay,
                            encoderPadding = audioMetadata.encoderPadding,
                            language = audioMetadata.language,
                            profile = audioMetadata.profile,
                            level = audioMetadata.level,
                            metadataWarning = audioMetadata.metadataWarning
                        )
                    )
                } catch (e: Exception) {
                    Log.e(
                        "AuralArc",
                        "Failed to read MediaStore track",
                        e
                    )
                }
            }
        }

        Log.d(
            "AuralArc",
            "MusicScanner found ${tracks.size} tracks"
        )

        return tracks
    }

    private fun cleanTrackNumber(
        rawTrackNumber: Int
    ): Int {
        if (
            rawTrackNumber <= 0
        ) {
            return 0
        }

        return if (
            rawTrackNumber >= 1000
        ) {
            rawTrackNumber % 1000
        } else {
            rawTrackNumber
        }
    }
}