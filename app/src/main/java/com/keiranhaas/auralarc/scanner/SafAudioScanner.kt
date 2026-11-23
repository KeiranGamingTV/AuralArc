package com.keiranhaas.auralarc.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.storage.PickedFolderStore
import com.keiranhaas.auralarc.utils.AudioMetadataReader
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

object SafAudioScanner {

    private const val TAG =
        "AuralArcSAF"

    /*
     * Files that are new or changed need their metadata read, which is
     * slow. Read several at once.
     */
    private const val PARALLEL_METADATA_READS =
        4

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

    /** One audio file found in a picked folder. */
    private class SafEntry(
        val uri: Uri,
        val name: String,
        val size: Long,
        val modifiedMillis: Long
    )

    suspend fun scan(
        context: Context,
        cachedTracksByUri: Map<String, MusicTrack> =
            emptyMap()
    ): List<MusicTrack> =
        coroutineScope {
            val entries =
                mutableListOf<SafEntry>()

            PickedFolderStore.getFolderUris(
                context
            ).forEach { uriString ->
                try {
                    val treeUri =
                        Uri.parse(
                            uriString
                        )

                    collectAudioFiles(
                        context = context,
                        treeUri = treeUri,
                        parentDocumentId =
                            DocumentsContract.getTreeDocumentId(
                                treeUri
                            ),
                        out = entries,
                        depth = 0,
                        isRoot = true
                    )
                } catch (e: Exception) {
                    Log.d(
                        TAG,
                        "Could not scan picked folder: $uriString",
                        e
                    )
                }
            }

            val tracks =
                mutableListOf<MusicTrack>()

            val pending =
                mutableListOf<Deferred<MusicTrack?>>()

            val gate =
                Semaphore(
                    PARALLEL_METADATA_READS
                )

            entries.forEach { entry ->
                val cachedTrack =
                    cachedTracksByUri[
                        entry.uri.toString()
                    ]

                if (
                    cachedTrack != null &&
                    entry.size > 0L &&
                    entry.modifiedMillis > 0L &&
                    cachedTrack.fileSizeBytes ==
                    entry.size &&
                    cachedTrack.dateModifiedMillis ==
                    entry.modifiedMillis
                ) {
                    tracks.add(
                        cachedTrack
                    )
                } else {
                    pending.add(
                        async(
                            Dispatchers.IO
                        ) {
                            gate.withPermit {
                                buildTrack(
                                    context = context,
                                    entry = entry
                                )
                            }
                        }
                    )
                }
            }

            pending.forEach { job ->
                job.await()?.let { track ->
                    tracks.add(
                        track
                    )
                }
            }

            tracks
        }

    /**
     * Lists a folder with ONE query per folder (name, type, size and
     * date together) instead of several separate queries per file, then
     * walks into its subfolders.
     */
    private fun collectAudioFiles(
        context: Context,
        treeUri: Uri,
        parentDocumentId: String,
        out: MutableList<SafEntry>,
        depth: Int,
        isRoot: Boolean = false
    ) {
        if (
            depth > 25
        ) {
            return
        }

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentDocumentId
            )

        val projection =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

        val subfolderIds =
            mutableListOf<String>()

        try {
            val cursor =
                context.contentResolver.query(
                    childrenUri,
                    projection,
                    null,
                    null,
                    null
                )

            if (
                cursor == null
            ) {
                if (
                    isRoot
                ) {
                    Log.d(
                        TAG,
                        "Picked folder unavailable: $treeUri"
                    )
                }

                return
            }

            cursor.use { c ->
                while (
                    c.moveToNext()
                ) {
                    val documentId =
                        c.getString(0)
                            ?: continue

                    val name =
                        c.getString(1).orEmpty()

                    val mimeType =
                        c.getString(2).orEmpty()

                    if (
                        mimeType ==
                        DocumentsContract.Document.MIME_TYPE_DIR
                    ) {
                        subfolderIds.add(
                            documentId
                        )
                    } else if (
                        isSupportedAudioFile(
                            name
                        )
                    ) {
                        out.add(
                            SafEntry(
                                uri =
                                    DocumentsContract.buildDocumentUriUsingTree(
                                        treeUri,
                                        documentId
                                    ),
                                name = name,
                                size =
                                    if (
                                        c.isNull(3)
                                    ) {
                                        0L
                                    } else {
                                        c.getLong(3)
                                    },
                                modifiedMillis =
                                    if (
                                        c.isNull(4)
                                    ) {
                                        0L
                                    } else {
                                        c.getLong(4)
                                    }
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(
                TAG,
                "Could not list SAF folder: $childrenUri",
                e
            )

            return
        }

        /*
         * The folder's cursor is closed before descending so only one
         * is open at a time.
         */
        subfolderIds.forEach { subfolderId ->
            collectAudioFiles(
                context = context,
                treeUri = treeUri,
                parentDocumentId = subfolderId,
                out = out,
                depth = depth + 1
            )
        }
    }

    private fun buildTrack(
        context: Context,
        entry: SafEntry
    ): MusicTrack? {
        val uri =
            entry.uri

        val fileSize =
            entry.size

        val modifiedTime =
            entry.modifiedMillis

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
            } ?: return fallbackTrackFromEntry(
                entry
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
                        entry.name.ifBlank {
                            "Unknown Title"
                        }
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
                        fallbackName = entry.name,
                        fileSizeBytes = fileSize,
                        dateModifiedMillis = modifiedTime,
                        sourceType = "Picked Folder / SAF",
                        sourcePath = uri.toString()
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

            fallbackTrackFromEntry(
                entry
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun fallbackTrackFromEntry(
        entry: SafEntry
    ): MusicTrack? {
        val name =
            entry.name

        if (
            name.isBlank() ||
            !isSupportedAudioFile(
                name
            )
        ) {
            return null
        }

        val uri =
            entry.uri

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