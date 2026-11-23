package com.keiranhaas.auralarc.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.utils.AudioMetadataReader
import com.keiranhaas.auralarc.utils.extractEmbeddedAlbumArt
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

object MusicScanner {

    /*
     * Files that are new or changed need their codec, bitrate and
     * artwork read, which is slow. Read several at once.
     */
    private const val PARALLEL_METADATA_READS =
        4

    suspend fun scan(
        context: Context,
        cachedTracksByUri: Map<String, MusicTrack> =
            emptyMap()
    ): List<MusicTrack> =
        coroutineScope {
            val tracks =
                mutableListOf<MusicTrack>()

            val pending =
                mutableListOf<Deferred<MusicTrack?>>()

            val gate =
                Semaphore(
                    PARALLEL_METADATA_READS
                )

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

            cursor?.use { c ->
                val idColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media._ID
                    )

                val displayNameColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DISPLAY_NAME
                    )

                val titleColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.TITLE
                    )

                val artistColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ARTIST
                    )

                val albumArtistColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ALBUM_ARTIST
                    )

                val albumColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ALBUM
                    )

                val durationColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DURATION
                    )

                val albumIdColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ALBUM_ID
                    )

                val trackColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.TRACK
                    )

                val yearColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.YEAR
                    )

                val sizeColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.SIZE
                    )

                val dateAddedColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DATE_ADDED
                    )

                val dateModifiedColumn =
                    c.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DATE_MODIFIED
                    )

                while (
                    c.moveToNext()
                ) {
                    try {
                        val id =
                            c.getLong(idColumn)

                        val displayName =
                            c.getString(displayNameColumn)

                        val rawTitle =
                            c.getString(titleColumn)

                        val rawArtist =
                            c.getString(artistColumn)

                        val rawAlbumArtist =
                            c.getString(albumArtistColumn)

                        val rawAlbum =
                            c.getString(albumColumn)

                        val duration =
                            c.getLong(durationColumn)

                        val albumId =
                            c.getLong(albumIdColumn)

                        val trackNumber =
                            cleanTrackNumber(
                                c.getInt(trackColumn)
                            )

                        val releaseYear =
                            c.getInt(yearColumn)
                                .coerceAtLeast(0)

                        val releaseDate =
                            if (
                                releaseYear > 0
                            ) {
                                releaseYear.toString()
                            } else {
                                ""
                            }

                        val fileSizeBytes =
                            c.getLong(sizeColumn)

                        val dateAddedMillis =
                            c.getLong(dateAddedColumn) * 1000L

                        val dateModifiedMillis =
                            c.getLong(dateModifiedColumn) * 1000L

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

                        val uriString =
                            uri.toString()

                        val cachedTrack =
                            cachedTracksByUri[
                                uriString
                            ]

                        if (
                            canReuseCachedMetadata(
                                cachedTrack = cachedTrack,
                                fileSizeBytes = fileSizeBytes,
                                dateModifiedMillis = dateModifiedMillis
                            )
                        ) {
                            tracks.add(
                                cachedTrack!!.copy(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    albumArtist =
                                        albumArtist,
                                    album = album,
                                    duration = duration,
                                    uri = uriString,
                                    albumId = albumId,
                                    trackNumber =
                                        trackNumber,
                                    releaseDate =
                                        releaseDate,
                                    releaseYear =
                                        releaseYear,
                                    fileSizeBytes =
                                        fileSizeBytes,
                                    dateAddedMillis =
                                        dateAddedMillis,
                                    dateModifiedMillis =
                                        dateModifiedMillis
                                )
                            )

                            continue
                        }

                        /*
                         * New or changed file: read its full metadata
                         * in the background while the cursor keeps
                         * moving. Each file is still handled
                         * independently, so one bad file can't affect
                         * the others.
                         */
                        pending.add(
                            async(
                                Dispatchers.IO
                            ) {
                                gate.withPermit {
                                    readFullTrack(
                                        context = context,
                                        id = id,
                                        uri = uri,
                                        displayName = displayName,
                                        title = title,
                                        artist = artist,
                                        albumArtist = albumArtist,
                                        album = album,
                                        duration = duration,
                                        albumId = albumId,
                                        trackNumber = trackNumber,
                                        releaseDate = releaseDate,
                                        releaseYear = releaseYear,
                                        fileSizeBytes = fileSizeBytes,
                                        dateAddedMillis = dateAddedMillis,
                                        dateModifiedMillis = dateModifiedMillis
                                    )
                                }
                            }
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

            pending.forEach { job ->
                job.await()?.let { track ->
                    tracks.add(
                        track
                    )
                }
            }

            Log.d(
                "AuralArc",
                "MusicScanner found ${tracks.size} tracks " +
                        "(${pending.size} new or changed)"
            )

            tracks
        }

    private fun canReuseCachedMetadata(
        cachedTrack: MusicTrack?,
        fileSizeBytes: Long,
        dateModifiedMillis: Long
    ): Boolean {
        if (
            cachedTrack == null ||
            fileSizeBytes <= 0L ||
            dateModifiedMillis <= 0L ||
            cachedTrack.fileSizeBytes != fileSizeBytes ||
            cachedTrack.dateModifiedMillis != dateModifiedMillis
        ) {
            return false
        }

        val artworkPath =
            cachedTrack.albumArtPath
                ?: return true

        return when {
            artworkPath.isBlank() ->
                true

            artworkPath.startsWith(
                "content://"
            ) ->
                true

            artworkPath.startsWith(
                "http://"
            ) ||
                    artworkPath.startsWith(
                        "https://"
                    ) ->
                true

            artworkPath.startsWith(
                "file://"
            ) ->
                File(
                    Uri.parse(
                        artworkPath
                    ).path.orEmpty()
                ).exists()

            else ->
                File(
                    artworkPath
                ).exists()
        }
    }

    private fun readFullTrack(
        context: Context,
        id: Long,
        uri: Uri,
        displayName: String?,
        title: String,
        artist: String,
        albumArtist: String,
        album: String,
        duration: Long,
        albumId: Long,
        trackNumber: Int,
        releaseDate: String,
        releaseYear: Int,
        fileSizeBytes: Long,
        dateAddedMillis: Long,
        dateModifiedMillis: Long
    ): MusicTrack? {
        return try {
            val audioMetadata =
                AudioMetadataReader.read(
                    context = context,
                    uri = uri,
                    fallbackName = displayName.orEmpty(),
                    fileSizeBytes = fileSizeBytes,
                    dateModifiedMillis = dateModifiedMillis,
                    sourceType = "Local MediaStore",
                    sourcePath = displayName ?: uri.toString()
                )

            val albumArtPath =
                extractEmbeddedAlbumArt(
                    context = context,
                    audioUriString = uri.toString(),
                    trackId = id,
                    albumId = albumId
                )

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
        } catch (e: Exception) {
            Log.e(
                "AuralArc",
                "Failed to read MediaStore track",
                e
            )

            null
        }
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