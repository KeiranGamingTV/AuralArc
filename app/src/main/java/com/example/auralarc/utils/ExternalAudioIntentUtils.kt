package com.example.auralarc.utils

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.auralarc.data.MusicTrack

fun getAudioUriFromIntent(
    intent: Intent?
): Uri? {
    if (
        intent == null
    ) {
        return null
    }

    return when (
        intent.action
    ) {
        Intent.ACTION_VIEW -> {
            intent.data
        }

        Intent.ACTION_SEND -> {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(
                Intent.EXTRA_STREAM
            )
        }

        else -> {
            null
        }
    }
}

fun createExternalAudioTrack(
    context: Context,
    uri: Uri
): MusicTrack {
    val uriString =
        uri.toString()

    val stableId =
        uriString.hashCode().toLong()

    val displayName =
        getDisplayNameFromUri(
            context,
            uri
        )

    var title: String? =
        null

    var artist: String? =
        null

    var album: String? =
        null

    var duration =
        0L

    var trackNumber =
        0

    val retriever =
        MediaMetadataRetriever()

    try {
        retriever.setDataSource(
            context,
            uri
        )

        title =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_TITLE
            )

        artist =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ARTIST
            )

        album =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ALBUM
            )

        duration =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
                ?.toLongOrNull()
                ?: 0L

        trackNumber =
            parseExternalTrackNumber(
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
                )
            )
    } catch (e: Exception) {
        Log.e(
            "AuralArc",
            "Failed to read external audio metadata: $uriString",
            e
        )
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }

    val cleanTitle =
        title
            ?.takeIf {
                it.isNotBlank()
            }
            ?: displayName
                ?.substringBeforeLast(
                    "."
                )
            ?: uri.lastPathSegment
            ?: "Opened Audio"

    val cleanArtist =
        artist
            ?.takeIf {
                it.isNotBlank() &&
                        it != "<unknown>"
            }
            ?: "Unknown Artist"

    val cleanAlbum =
        album
            ?.takeIf {
                it.isNotBlank() &&
                        it != "<unknown>"
            }
            ?: "External Audio"

    val albumArtPath =
        extractEmbeddedAlbumArt(
            context = context,
            audioUriString = uriString,
            trackId = stableId
        )

    Log.d(
        "AuralArc",
        "External audio opened: title=$cleanTitle uri=$uriString art=$albumArtPath"
    )

    return MusicTrack(
        id = stableId,
        title = cleanTitle,
        artist = cleanArtist,
        album = cleanAlbum,
        albumArtist = cleanArtist,
        duration = duration,
        uri = uriString,
        albumId = stableId,
        trackNumber = trackNumber,
        albumArtPath = albumArtPath
    )
}

private fun getDisplayNameFromUri(
    context: Context,
    uri: Uri
): String? {
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
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        OpenableColumns.DISPLAY_NAME
                    )
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun parseExternalTrackNumber(
    value: String?
): Int {
    val rawNumber =
        value
            ?.substringBefore(
                "/"
            )
            ?.trim()
            ?.toIntOrNull()
            ?: return 0

    return if (
        rawNumber >= 1000
    ) {
        rawNumber % 1000
    } else {
        rawNumber
    }
}