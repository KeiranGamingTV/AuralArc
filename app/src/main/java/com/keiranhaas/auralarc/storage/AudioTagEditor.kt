package com.keiranhaas.auralarc.storage

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.utils.clearCachedEmbeddedAlbumArt
import com.keiranhaas.auralarc.utils.extractEmbeddedAlbumArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.reference.PictureTypes
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.vorbiscomment.util.Base64Coder

data class EditableTrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?
)

sealed class AudioTagEditResult {

    data class Success(
        val updatedTracks: List<MusicTrack>,
        val message: String
    ) : AudioTagEditResult()

    data class PermissionRequired(
        val pendingIntent: PendingIntent
    ) : AudioTagEditResult()

    data class Failure(
        val message: String
    ) : AudioTagEditResult()
}

object AudioTagEditor {

    private const val TEMP_DIRECTORY =
        "audio_tag_editor"

    private const val BUFFER_SIZE =
        256 * 1024

    private data class PreparedArtwork(
        val binaryData: ByteArray,
        val mimeType: String,
        val width: Int,
        val height: Int
    )

    fun metadataFromTrack(
        track: MusicTrack
    ): EditableTrackMetadata {
        return EditableTrackMetadata(
            title = track.title,
            artist =
            track.artist.takeUnless {
                it.equals(
                    "Unknown Artist",
                    ignoreCase = true
                )
            }.orEmpty(),
            album =
            track.album.takeUnless {
                it.equals(
                    "Unknown Album",
                    ignoreCase = true
                )
            }.orEmpty(),
            albumArtist =
            track.albumArtist.takeUnless {
                it.equals(
                    "Unknown Artist",
                    ignoreCase = true
                )
            }.orEmpty(),
            genre = track.genre,
            year =
            track.releaseYear.takeIf {
                it > 0
            },
            trackNumber =
            track.trackNumber.takeIf {
                it > 0
            },
            discNumber =
            track.discNumber.takeIf {
                it > 0
            }
        )
    }

    fun isTrackEditable(
        track: MusicTrack
    ): Boolean {
        if (
            track.sourceType.contains(
                "Navidrome",
                ignoreCase = true
            )
        ) {
            return false
        }

        val uri =
            parseTrackUri(
                track
            ) ?: return false

        return when (
            uri.scheme?.lowercase()
        ) {
            "content",
            "file",
            null,
            "" -> true

            else -> false
        }
    }

    fun readOnlyReason(
        track: MusicTrack
    ): String {
        return when {
            track.sourceType.contains(
                "Navidrome",
                ignoreCase = true
            ) -> {
                "Navidrome tracks are read-only in AuralArc. Edit their metadata on the server, then refresh the Navidrome library."
            }

            track.uri.startsWith(
                "http://",
                ignoreCase = true
            ) ||
                    track.uri.startsWith(
                        "https://",
                        ignoreCase = true
                    ) -> {
                "Remote streams cannot be edited as local audio files."
            }

            else -> {
                "AuralArc cannot obtain a writable local file from this track’s source."
            }
        }
    }

    /**
     * Returns a system permission request when MediaStore owns
     * one or more of the files.
     *
     * SAF files normally use their existing persisted folder
     * permission and do not need this request.
     */
    fun createWriteRequest(
        context: Context,
        tracks: List<MusicTrack>
    ): PendingIntent? {
        val mediaStoreUris =
            tracks
                .asSequence()
                .mapNotNull { track ->
                    parseTrackUri(
                        track
                    )
                }
                .filter { uri ->
                    isMediaStoreUri(
                        uri
                    )
                }
                .distinct()
                .toList()

        if (
            mediaStoreUris.isEmpty()
        ) {
            return null
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {
            return MediaStore.createWriteRequest(
                context.contentResolver,
                mediaStoreUris
            )
        }

        /*
         * Android 10 supplies its permission request through
         * RecoverableSecurityException. Opening in rw mode
         * discovers that requirement without changing the file.
         */
        mediaStoreUris.forEach { uri ->
            try {
                context.contentResolver
                    .openFileDescriptor(
                        uri,
                        "rw"
                    )
                    ?.close()
            } catch (
                exception: RecoverableSecurityException
            ) {
                return exception
                    .userAction
                    .actionIntent
            } catch (_: Exception) {
            }
        }

        return null
    }

    suspend fun updateTrackMetadata(
        context: Context,
        track: MusicTrack,
        metadata: EditableTrackMetadata
    ): AudioTagEditResult {
        return withContext(
            Dispatchers.IO
        ) {
            if (
                !isTrackEditable(
                    track
                )
            ) {
                return@withContext AudioTagEditResult.Failure(
                    readOnlyReason(
                        track
                    )
                )
            }

            if (
                metadata.title.isBlank()
            ) {
                return@withContext AudioTagEditResult.Failure(
                    "The song title cannot be blank."
                )
            }

            try {
                rewriteAudioFile(
                    context = context,
                    track = track
                ) { workingFile ->
                    val audioFile =
                        AudioFileIO.read(
                            workingFile
                        )

                    val tag =
                        audioFile
                            .getTagOrCreateAndSetDefault()

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.TITLE,
                        value = metadata.title.trim()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.ARTIST,
                        value = metadata.artist.trim()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.ALBUM,
                        value = metadata.album.trim()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey =
                        FieldKey.ALBUM_ARTIST,
                        value =
                        metadata.albumArtist.trim()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.GENRE,
                        value = metadata.genre.trim()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.YEAR,
                        value =
                        metadata.year
                            ?.toString()
                            .orEmpty()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.TRACK,
                        value =
                        metadata.trackNumber
                            ?.toString()
                            .orEmpty()
                    )

                    setOrDelete(
                        tag = tag,
                        fieldKey = FieldKey.DISC_NO,
                        value =
                        metadata.discNumber
                            ?.toString()
                            .orEmpty()
                    )

                    AudioFileIO.write(
                        audioFile
                    )
                }

                updateMediaStoreMetadata(
                    context = context,
                    track = track,
                    metadata = metadata
                )

                notifyFileChanged(
                    context = context,
                    track = track
                )

                val updatedTrack =
                    track.copy(
                        title =
                        metadata.title.trim(),
                        artist =
                        metadata.artist
                            .trim()
                            .ifBlank {
                                "Unknown Artist"
                            },
                        album =
                        metadata.album
                            .trim()
                            .ifBlank {
                                "Unknown Album"
                            },
                        albumArtist =
                        metadata.albumArtist
                            .trim()
                            .ifBlank {
                                metadata.artist
                                    .trim()
                                    .ifBlank {
                                        "Unknown Artist"
                                    }
                            },
                        genre =
                        metadata.genre.trim(),
                        releaseYear =
                        metadata.year ?: 0,
                        releaseDate =
                        metadata.year
                            ?.toString()
                            .orEmpty(),
                        trackNumber =
                        metadata.trackNumber ?: 0,
                        discNumber =
                        metadata.discNumber ?: 0,
                        dateModifiedMillis =
                        System.currentTimeMillis()
                    )

                AudioTagEditResult.Success(
                    updatedTracks =
                    listOf(
                        updatedTrack
                    ),
                    message =
                    "Metadata was saved successfully."
                )
            } catch (
                exception: RecoverableSecurityException
            ) {
                AudioTagEditResult.PermissionRequired(
                    pendingIntent =
                    exception
                        .userAction
                        .actionIntent
                )
            } catch (
                exception: SecurityException
            ) {
                AudioTagEditResult.Failure(
                    "Android denied write access to this file. Re-add its folder through AuralArc’s Folder Picker with write access, or approve Android’s write request."
                )
            } catch (
                exception: Exception
            ) {
                AudioTagEditResult.Failure(
                    editFailureMessage(
                        operation = "Metadata editing",
                        track = track,
                        exception = exception
                    )
                )
            }
        }
    }

    suspend fun addAlbumArtwork(
        context: Context,
        tracks: List<MusicTrack>,
        imageUri: Uri
    ): AudioTagEditResult {
        return withContext(
            Dispatchers.IO
        ) {
            if (
                tracks.isEmpty()
            ) {
                return@withContext AudioTagEditResult.Failure(
                    "This album does not contain any tracks."
                )
            }

            if (
                tracks.any { track ->
                    !track.albumArtPath.isNullOrBlank()
                }
            ) {
                return@withContext AudioTagEditResult.Failure(
                    "This album already has artwork. AuralArc only exposes this action for albums with no artwork."
                )
            }

            val editableTracks =
                tracks.filter { track ->
                    isTrackEditable(
                        track
                    )
                }

            if (
                editableTracks.isEmpty()
            ) {
                return@withContext AudioTagEditResult.Failure(
                    "None of this album’s tracks are writable local files."
                )
            }

            val tempDirectory =
                editorTempDirectory(
                    context
                )

            val imageExtension =
                imageExtension(
                    context = context,
                    imageUri = imageUri
                )

            val temporaryImage =
                File(
                    tempDirectory,
                    "selected_art_${UUID.randomUUID()}.$imageExtension"
                )

            try {
                copyUriToFile(
                    context = context,
                    uri = imageUri,
                    targetFile = temporaryImage
                )

                val preparedArtwork =
                    prepareArtwork(
                        temporaryImage
                    )

                val previewPath =
                    createPersistentArtworkPreview(
                        context = context,
                        imageFile = temporaryImage,
                        extension = imageExtension
                    )

                val updatedTracks =
                    mutableListOf<MusicTrack>()

                val failedTracks =
                    mutableListOf<String>()

                editableTracks.forEach { track ->
                    try {
                        rewriteAudioFile(
                            context = context,
                            track = track
                        ) { workingFile ->
                            val audioFile =
                                AudioFileIO.read(
                                    workingFile
                                )

                            val tag =
                                audioFile
                                    .getTagOrCreateAndSetDefault()

                            writeArtworkToTag(
                                tag = tag,
                                artwork = preparedArtwork
                            )

                            AudioFileIO.write(
                                audioFile
                            )
                        }

                        clearCachedEmbeddedAlbumArt(
                            context = context,
                            trackId = track.id
                        )

                        val extractedArtworkPath =
                            extractEmbeddedAlbumArt(
                                context = context,
                                audioUriString =
                                track.uri,
                                trackId = track.id
                            )

                        updatedTracks.add(
                            track.copy(
                                albumArtPath =
                                extractedArtworkPath
                                    ?: previewPath,
                                dateModifiedMillis =
                                System.currentTimeMillis()
                            )
                        )

                        notifyFileChanged(
                            context = context,
                            track = track
                        )
                    } catch (
                        exception: RecoverableSecurityException
                    ) {
                        return@withContext AudioTagEditResult.PermissionRequired(
                            pendingIntent =
                            exception
                                .userAction
                                .actionIntent
                        )
                    } catch (
                        exception: Exception
                    ) {
                        failedTracks.add(
                            buildString {
                                append(
                                    track.title
                                )

                                append(
                                    ": "
                                )

                                append(
                                    exception.javaClass
                                        .simpleName
                                )

                                val detail =
                                    exception.message
                                        ?.trim()
                                        .orEmpty()

                                if (
                                    detail.isNotBlank()
                                ) {
                                    append(
                                        " — "
                                    )

                                    append(
                                        detail
                                    )
                                }
                            }
                        )
                    }
                }

                val readOnlyCount =
                    tracks.size -
                            editableTracks.size

                if (
                    updatedTracks.isEmpty()
                ) {
                    return@withContext AudioTagEditResult.Failure(
                        buildString {
                            append(
                                "Artwork could not be written to any album tracks."
                            )

                            if (
                                failedTracks.isNotEmpty()
                            ) {
                                append(
                                    "\n\n"
                                )

                                append(
                                    failedTracks
                                        .take(
                                            5
                                        )
                                        .joinToString(
                                            "\n"
                                        )
                                )
                            }
                        }
                    )
                }

                val message =
                    buildString {
                        append(
                            "Artwork was added to "
                        )

                        append(
                            updatedTracks.size
                        )

                        append(
                            if (
                                updatedTracks.size == 1
                            ) {
                                " track."
                            } else {
                                " tracks."
                            }
                        )

                        if (
                            readOnlyCount > 0
                        ) {
                            append(
                                "\n$readOnlyCount read-only track"
                            )

                            if (
                                readOnlyCount != 1
                            ) {
                                append(
                                    "s"
                                )
                            }

                            append(
                                " skipped."
                            )
                        }

                        if (
                            failedTracks.isNotEmpty()
                        ) {
                            append(
                                "\n${failedTracks.size} track"
                            )

                            if (
                                failedTracks.size != 1
                            ) {
                                append(
                                    "s"
                                )
                            }

                            append(
                                " failed."
                            )
                        }
                    }

                AudioTagEditResult.Success(
                    updatedTracks =
                    updatedTracks,
                    message = message
                )
            } catch (
                exception: SecurityException
            ) {
                AudioTagEditResult.Failure(
                    "AuralArc could not read the selected image or write the album files because Android denied access."
                )
            } catch (
                exception: Exception
            ) {
                AudioTagEditResult.Failure(
                    "The selected image could not be used: " +
                            (
                                    exception.message
                                        ?: exception.javaClass
                                            .simpleName
                                    )
                )
            } finally {
                try {
                    temporaryImage.delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun rewriteAudioFile(
        context: Context,
        track: MusicTrack,
        editWorkingFile: (File) -> Unit
    ) {
        val trackUri =
            parseTrackUri(
                track
            ) ?: throw IllegalArgumentException(
                "Invalid track URI."
            )

        val extension =
            audioFileExtension(
                context = context,
                track = track,
                uri = trackUri
            )

        if (
            extension.isBlank()
        ) {
            throw IllegalArgumentException(
                "The file extension could not be determined."
            )
        }

        val tempDirectory =
            editorTempDirectory(
                context
            )

        val uniqueId =
            UUID.randomUUID()
                .toString()

        val originalCopy =
            File(
                tempDirectory,
                "original_${uniqueId}.$extension"
            )

        val workingCopy =
            File(
                tempDirectory,
                "working_${uniqueId}.$extension"
            )

        try {
            copyUriToFile(
                context = context,
                uri = trackUri,
                targetFile = originalCopy
            )

            originalCopy.copyTo(
                target = workingCopy,
                overwrite = true
            )

            editWorkingFile(
                workingCopy
            )

            /*
             * Re-read the edited copy before replacing the
             * original. This catches many malformed writes
             * before the source file is touched.
             */
            AudioFileIO.read(
                workingCopy
            )

            try {
                copyFileToUri(
                    context = context,
                    sourceFile = workingCopy,
                    uri = trackUri
                )
            } catch (
                writeException: Exception
            ) {
                /*
                 * The original bytes are retained in a separate
                 * temporary file. Attempt to restore them if the
                 * final copy-back fails after truncation.
                 */
                try {
                    copyFileToUri(
                        context = context,
                        sourceFile = originalCopy,
                        uri = trackUri
                    )
                } catch (
                    restoreException: Exception
                ) {
                    writeException.addSuppressed(
                        restoreException
                    )
                }

                throw writeException
            }

            context.contentResolver.notifyChange(
                trackUri,
                null
            )
        } finally {
            try {
                originalCopy.delete()
            } catch (_: Exception) {
            }

            try {
                workingCopy.delete()
            } catch (_: Exception) {
            }
        }
    }

    private fun setOrDelete(
        tag: Tag,
        fieldKey: FieldKey,
        value: String
    ) {
        if (
            value.isBlank()
        ) {
            try {
                tag.deleteField(
                    fieldKey
                )
            } catch (_: Exception) {
            }

            return
        }

        tag.setField(
            fieldKey,
            value
        )
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        targetFile: File
    ) {
        openInputStream(
            context = context,
            uri = uri
        ).use { inputStream ->
            FileOutputStream(
                targetFile
            ).use { outputStream ->
                inputStream.copyTo(
                    outputStream,
                    BUFFER_SIZE
                )

                outputStream.flush()
            }
        }

        if (
            !targetFile.exists() ||
            targetFile.length() <= 0L
        ) {
            throw IllegalStateException(
                "The source file could not be copied."
            )
        }
    }

    private fun copyFileToUri(
        context: Context,
        sourceFile: File,
        uri: Uri
    ) {
        openOutputStreamForRewrite(
            context = context,
            uri = uri
        ).use { outputStream ->
            FileInputStream(
                sourceFile
            ).use { inputStream ->
                inputStream.copyTo(
                    outputStream,
                    BUFFER_SIZE
                )

                outputStream.flush()
            }
        }
    }

    private fun openInputStream(
        context: Context,
        uri: Uri
    ): InputStream {
        return when (
            uri.scheme?.lowercase()
        ) {
            "file" -> {
                FileInputStream(
                    File(
                        uri.path
                            ?: throw IllegalArgumentException(
                                "Missing file path."
                            )
                    )
                )
            }

            null,
            "" -> {
                FileInputStream(
                    File(
                        uri.toString()
                    )
                )
            }

            else -> {
                context.contentResolver
                    .openInputStream(
                        uri
                    )
                    ?: throw IllegalStateException(
                        "Android could not open the source file."
                    )
            }
        }
    }

    private fun openOutputStreamForRewrite(
        context: Context,
        uri: Uri
    ): OutputStream {
        return when (
            uri.scheme?.lowercase()
        ) {
            "file" -> {
                FileOutputStream(
                    File(
                        uri.path
                            ?: throw IllegalArgumentException(
                                "Missing file path."
                            )
                    ),
                    false
                )
            }

            null,
            "" -> {
                FileOutputStream(
                    File(
                        uri.toString()
                    ),
                    false
                )
            }

            else -> {
                try {
                    context.contentResolver
                        .openOutputStream(
                            uri,
                            "rwt"
                        )
                } catch (
                    exception: IllegalArgumentException
                ) {
                    context.contentResolver
                        .openOutputStream(
                            uri,
                            "wt"
                        )
                }
                    ?: throw IllegalStateException(
                        "Android could not open the destination for writing."
                    )
            }
        }
    }

    private fun editorTempDirectory(
        context: Context
    ): File {
        return File(
            context.cacheDir,
            TEMP_DIRECTORY
        ).apply {
            if (
                !exists()
            ) {
                mkdirs()
            }
        }
    }

    private fun parseTrackUri(
        track: MusicTrack
    ): Uri? {
        return try {
            Uri.parse(
                track.uri
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun isMediaStoreUri(
        uri: Uri
    ): Boolean {
        return uri.scheme.equals(
            "content",
            ignoreCase = true
        ) &&
                uri.authority.equals(
                    MediaStore.AUTHORITY,
                    ignoreCase = true
                )
    }

    private fun audioFileExtension(
        context: Context,
        track: MusicTrack,
        uri: Uri
    ): String {
        val storedExtension =
            cleanExtension(
                track.fileExtension
            )

        if (
            storedExtension.isNotBlank()
        ) {
            return storedExtension
        }

        val displayName =
            queryDisplayName(
                context = context,
                uri = uri
            )

        val displayExtension =
            cleanExtension(
                displayName
                    .substringAfterLast(
                        ".",
                        ""
                    )
            )

        if (
            displayExtension.isNotBlank()
        ) {
            return displayExtension
        }

        return cleanExtension(
            uri.lastPathSegment
                ?.substringAfterLast(
                    ".",
                    ""
                )
                .orEmpty()
        )
    }

    private fun imageExtension(
        context: Context,
        imageUri: Uri
    ): String {
        val mimeType =
            context.contentResolver
                .getType(
                    imageUri
                )
                .orEmpty()
                .lowercase()

        when (
            mimeType
        ) {
            "image/png" ->
                return "png"

            "image/webp" ->
                return "webp"

            "image/gif" ->
                return "gif"

            "image/jpeg",
            "image/jpg" ->
                return "jpg"
        }

        val displayName =
            queryDisplayName(
                context = context,
                uri = imageUri
            )

        return cleanExtension(
            displayName.substringAfterLast(
                ".",
                "jpg"
            )
        ).ifBlank {
            "jpg"
        }
    }

    private fun cleanExtension(
        value: String
    ): String {
        return value
            .trim()
            .removePrefix(
                "."
            )
            .lowercase()
            .filter { character ->
                character.isLetterOrDigit()
            }
            .take(
                8
            )
    }

    private fun queryDisplayName(
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
                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (
                    index >= 0 &&
                    cursor.moveToFirst()
                ) {
                    cursor.getString(
                        index
                    ).orEmpty()
                } else {
                    ""
                }
            }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun prepareArtwork(
        imageFile: File
    ): PreparedArtwork {
        val options =
            BitmapFactory.Options().apply {
                inJustDecodeBounds =
                    true
            }

        BitmapFactory.decodeFile(
            imageFile.absolutePath,
            options
        )

        if (
            options.outWidth <= 0 ||
            options.outHeight <= 0
        ) {
            throw IllegalArgumentException(
                "The selected file is not a readable image."
            )
        }

        val binaryData =
            imageFile.readBytes()

        if (
            binaryData.isEmpty()
        ) {
            throw IllegalArgumentException(
                "The selected image is empty."
            )
        }

        val detectedMimeType =
            options.outMimeType
                ?.trim()
                ?.lowercase()
                .orEmpty()

        val mimeType =
            when {
                detectedMimeType.startsWith(
                    "image/"
                ) -> {
                    detectedMimeType
                }

                imageFile.extension.equals(
                    "png",
                    ignoreCase = true
                ) -> {
                    "image/png"
                }

                imageFile.extension.equals(
                    "webp",
                    ignoreCase = true
                ) -> {
                    "image/webp"
                }

                else -> {
                    "image/jpeg"
                }
            }

        return PreparedArtwork(
            binaryData = binaryData,
            mimeType = mimeType,
            width = options.outWidth,
            height = options.outHeight
        )
    }

    private fun writeArtworkToTag(
        tag: Tag,
        artwork: PreparedArtwork
    ) {
        try {
            tag.deleteArtworkField()
        } catch (_: Exception) {
            /*
             * Continue when the file simply has no existing
             * artwork field to remove.
             */
        }

        when (
            tag
        ) {
            /*
             * The Android Jaudiotagger fork's generic FLAC artwork
             * method calls AndroidArtwork.setImageFromData(), which
             * throws UnsupportedOperationException.
             *
             * Construct the native FLAC picture block directly.
             */
            is FlacTag -> {
                val pictureField =
                    tag.createArtworkField(
                        artwork.binaryData,
                        PictureTypes.DEFAULT_ID,
                        artwork.mimeType,
                        "Front Cover",
                        artwork.width,
                        artwork.height,
                        0,
                        0
                    )

                tag.setField(
                    pictureField
                )
            }

            /*
             * Vorbis/OGG has the same broken generic conversion.
             * Construct and Base64-encode a standard
             * METADATA_BLOCK_PICTURE value directly.
             */
            is VorbisCommentTag -> {
                val pictureBlock =
                    MetadataBlockDataPicture(
                        artwork.binaryData,
                        PictureTypes.DEFAULT_ID,
                        artwork.mimeType,
                        "Front Cover",
                        artwork.width,
                        artwork.height,
                        0,
                        0
                    )

                val encodedPicture =
                    String(
                        Base64Coder.encode(
                            pictureBlock.getRawContent()
                        )
                    )

                val pictureField =
                    tag.createField(
                        VorbisCommentFieldKey
                            .METADATA_BLOCK_PICTURE,
                        encodedPicture
                    )

                tag.setField(
                    pictureField
                )
            }

            /*
             * MP3 and MP4 implementations use the binary artwork
             * data directly and do not require the unsupported
             * Android image-conversion method.
             */
            else -> {
                val genericArtwork =
                    ArtworkFactory.getNew()

                genericArtwork.setBinaryData(
                    artwork.binaryData
                )

                genericArtwork.setMimeType(
                    artwork.mimeType
                )

                genericArtwork.setDescription(
                    "Front Cover"
                )

                genericArtwork.setPictureType(
                    PictureTypes.DEFAULT_ID
                )

                genericArtwork.setWidth(
                    artwork.width
                )

                genericArtwork.setHeight(
                    artwork.height
                )

                tag.setField(
                    genericArtwork
                )
            }
        }
    }

    private fun createPersistentArtworkPreview(
        context: Context,
        imageFile: File,
        extension: String
    ): String {
        val artDirectory =
            File(
                context.cacheDir,
                "album_art"
            ).apply {
                if (
                    !exists()
                ) {
                    mkdirs()
                }
            }

        val previewFile =
            File(
                artDirectory,
                "manual_${UUID.randomUUID()}.$extension"
            )

        imageFile.copyTo(
            target = previewFile,
            overwrite = true
        )

        return previewFile.absolutePath
    }

    private fun updateMediaStoreMetadata(
        context: Context,
        track: MusicTrack,
        metadata: EditableTrackMetadata
    ) {
        val uri =
            parseTrackUri(
                track
            ) ?: return

        if (
            !isMediaStoreUri(
                uri
            )
        ) {
            return
        }

        try {
            val values =
                ContentValues().apply {
                    put(
                        MediaStore.Audio.Media.TITLE,
                        metadata.title.trim()
                    )

                    put(
                        MediaStore.Audio.Media.ARTIST,
                        metadata.artist.trim()
                    )

                    put(
                        MediaStore.Audio.Media.ALBUM,
                        metadata.album.trim()
                    )

                    put(
                        MediaStore.Audio.Media.ALBUM_ARTIST,
                        metadata.albumArtist.trim()
                    )

                    put(
                        MediaStore.Audio.Media.YEAR,
                        metadata.year ?: 0
                    )

                    put(
                        MediaStore.Audio.Media.TRACK,
                        metadata.trackNumber ?: 0
                    )

                    put(
                        MediaStore.Audio.Media.DATE_MODIFIED,
                        System.currentTimeMillis() /
                                1000L
                    )
                }

            context.contentResolver.update(
                uri,
                values,
                null,
                null
            )
        } catch (_: Exception) {
            /*
             * The embedded file tags were already written.
             * MediaStore will receive another opportunity to
             * update when the file is scanned.
             */
        }
    }

    private fun notifyFileChanged(
        context: Context,
        track: MusicTrack
    ) {
        val uri =
            parseTrackUri(
                track
            ) ?: return

        context.contentResolver.notifyChange(
            uri,
            null
        )

        val filePath =
            mediaStoreDataPath(
                context = context,
                uri = uri
            )

        if (
            filePath.isNotBlank()
        ) {
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(
                        filePath
                    ),
                    null,
                    null
                )
            } catch (_: Exception) {
            }
        }
    }

    @Suppress(
        "DEPRECATION"
    )
    private fun mediaStoreDataPath(
        context: Context,
        uri: Uri
    ): String {
        if (
            !isMediaStoreUri(
                uri
            )
        ) {
            return ""
        }

        return try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DATA
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val index =
                    cursor.getColumnIndex(
                        MediaStore.MediaColumns.DATA
                    )

                if (
                    index >= 0 &&
                    cursor.moveToFirst()
                ) {
                    cursor.getString(
                        index
                    ).orEmpty()
                } else {
                    ""
                }
            }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun editFailureMessage(
        operation: String,
        track: MusicTrack,
        exception: Exception
    ): String {
        return buildString {
            append(
                operation
            )

            append(
                " failed for “"
            )

            append(
                track.title
            )

            append(
                "”."
            )

            val detail =
                exception.message
                    ?.trim()
                    .orEmpty()

            if (
                detail.isNotBlank()
            ) {
                append(
                    "\n\n"
                )

                append(
                    detail
                )
            } else {
                append(
                    "\n\n"
                )

                append(
                    exception.javaClass
                        .simpleName
                )
            }
        }
    }
}