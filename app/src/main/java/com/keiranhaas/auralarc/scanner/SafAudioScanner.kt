package com.keiranhaas.auralarc.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.storage.PickedFolderStore
import java.io.File
import com.keiranhaas.auralarc.utils.AudioMetadataReader

object SafAudioScanner {

    private const val TAG =
        "AuralArcSAF"

    private val supportedExtensions =
        setOf(
            "mp3",
            "flac",
            "wav",
            "ogg",
            "opus",
            "m4a",
            "aac",
            "alac",
            "aiff",
            "aif",
            "wma",
            "mp4",
            "3gp",
            "amr"
        )

    fun scan(
        context: Context
    ): List<MusicTrack> {
        val tracks =
            mutableListOf<MusicTrack>()

        PickedFolderStore.getFolderUris(
            context
        ).forEach { uriString ->
            try {
                val rootUri =
                    Uri.parse(
                        uriString
                    )

                val root =
                    DocumentFile.fromTreeUri(
                        context,
                        rootUri
                    )

                if (
                    root == null ||
                    !root.exists() ||
                    !root.isDirectory
                ) {
                    Log.d(
                        TAG,
                        "Picked folder unavailable: $uriString"
                    )

                    return@forEach
                }

                scanFolder(
                    context = context,
                    folder = root,
                    tracks = tracks,
                    depth = 0
                )
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "Could not scan picked folder: $uriString",
                    e
                )
            }
        }

        return tracks
    }

    private fun scanFolder(
        context: Context,
        folder: DocumentFile,
        tracks: MutableList<MusicTrack>,
        depth: Int
    ) {
        if (
            depth > 25
        ) {
            return
        }

        val children =
            try {
                folder.listFiles()
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "Could not list SAF folder: ${folder.uri}",
                    e
                )

                return
            }

        children.forEach { file ->
            try {
                when {
                    file.isDirectory -> {
                        scanFolder(
                            context = context,
                            folder = file,
                            tracks = tracks,
                            depth = depth + 1
                        )
                    }

                    file.isFile &&
                            isSupportedAudioFile(
                                file.name ?: ""
                            ) -> {
                        buildTrack(
                            context = context,
                            file = file
                        )?.let { track ->
                            tracks.add(
                                track
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "Could not scan SAF item: ${file.uri}",
                    e
                )
            }
        }
    }

    private fun buildTrack(
        context: Context,
        file: DocumentFile
    ): MusicTrack? {
        val uri =
            file.uri

        val retriever =
            MediaMetadataRetriever()

        val parcelFileDescriptor =
            try {
                context.contentResolver.openFileDescriptor(
                    uri,
                    "r"
                )
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "Could not open SAF file read-only: $uri",
                    e
                )

                null
            } ?: return fallbackTrackFromFile(
                file
            )

        return try {
            parcelFileDescriptor.use { pfd ->
                retriever.setDataSource(
                    pfd.fileDescriptor
                )

                val title =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_TITLE
                    )?.takeIf {
                        it.isNotBlank()
                    } ?: cleanTitleFromFileName(
                        file.name ?: "Unknown Title"
                    )

                val artist =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_ARTIST
                    )?.takeIf {
                        it.isNotBlank()
                    } ?: "Unknown Artist"

                val albumArtist =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST
                    )?.takeIf {
                        it.isNotBlank() &&
                                !it.equals(
                                    "<unknown>",
                                    ignoreCase = true
                                )
                    } ?: artist

                val album =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_ALBUM
                    )?.takeIf {
                        it.isNotBlank()
                    } ?: "Unknown Album"

                val duration =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L

                val trackNumber =
                    parseTrackNumber(
                        retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
                        )
                    )

                val releaseDate =
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_YEAR
                    )?.trim()
                        ?: ""

                val releaseYear =
                    extractReleaseYear(
                        releaseDate
                    )

                val audioMetadata =
                    AudioMetadataReader.read(
                        context = context,
                        uri = uri,
                        fallbackName = file.name ?: "",
                        fileSizeBytes = file.length(),
                        dateModifiedMillis = file.lastModified(),
                        sourceType = "Picked Folder / SAF",
                        sourcePath = file.uri.toString()
                    )

                val id =
                    positiveId(
                        uri.toString()
                    )

                val albumArtPath =
                    extractAlbumArt(
                        context = context,
                        retriever = retriever,
                        id = id
                    )

                MusicTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = audioMetadata.genre,
                    duration = duration,
                    uri = uri.toString(),
                    albumId = 0L,
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
            }
        } catch (e: Exception) {
            Log.d(
                TAG,
                "Could not read SAF metadata, using filename fallback: $uri",
                e
            )

            fallbackTrackFromFile(
                file
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun fallbackTrackFromFile(
        file: DocumentFile
    ): MusicTrack? {
        val name =
            file.name ?: return null

        if (
            !isSupportedAudioFile(
                name
            )
        ) {
            return null
        }

        val uri =
            file.uri

        val title =
            cleanTitleFromFileName(
                name
            )

        val id =
            positiveId(
                uri.toString()
            )

        return MusicTrack(
            id = id,
            title = title,
            artist = "Unknown Artist",
            album = "Unknown Album",
            albumArtist = "Unknown Artist",
            duration = 0L,
            uri = uri.toString(),
            albumId = 0L,
            trackNumber = 0,
            albumArtPath = null,
            releaseDate = "",
            releaseYear = 0
        )
    }

    private fun extractAlbumArt(
        context: Context,
        retriever: MediaMetadataRetriever,
        id: Long
    ): String? {
        return try {
            val bytes =
                retriever.embeddedPicture ?: return null

            if (
                bytes.isEmpty()
            ) {
                return null
            }

            val cacheDir =
                File(
                    context.cacheDir,
                    "album_art"
                )

            if (
                !cacheDir.exists()
            ) {
                cacheDir.mkdirs()
            }

            val artFile =
                File(
                    cacheDir,
                    "saf_track_$id.jpg"
                )

            if (
                !artFile.exists() ||
                artFile.length() <= 0L
            ) {
                artFile.writeBytes(
                    bytes
                )
            }

            artFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun isSupportedAudioFile(
        name: String
    ): Boolean {
        val extension =
            name.substringAfterLast(
                '.',
                ""
            ).lowercase()

        return supportedExtensions.contains(
            extension
        )
    }

    private fun cleanTitleFromFileName(
        name: String
    ): String {
        return name.substringBeforeLast(
            "."
        ).ifBlank {
            name
        }
    }

    private fun parseTrackNumber(
        rawValue: String?
    ): Int {
        if (
            rawValue.isNullOrBlank()
        ) {
            return 0
        }

        return rawValue
            .substringBefore(
                "/"
            )
            .filter {
                it.isDigit()
            }
            .toIntOrNull()
            ?: 0
    }

    private fun extractReleaseYear(
        rawValue: String
    ): Int {
        return Regex("""\d{4}""")
            .find(
                rawValue
            )
            ?.value
            ?.toIntOrNull()
            ?.coerceAtLeast(
                0
            )
            ?: 0
    }

    private fun positiveId(
        value: String
    ): Long {
        val id =
            value.hashCode().toLong()

        return if (
            id < 0L
        ) {
            -id
        } else {
            id
        }
    }
}