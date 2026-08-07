package com.example.auralarc.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

fun extractEmbeddedAlbumArt(
    context: Context,
    audioUriString: String,
    trackId: Long
): String? {
    val artDirectory =
        File(
            context.cacheDir,
            "album_art"
        )

    if (
        !artDirectory.exists()
    ) {
        artDirectory.mkdirs()
    }

    val artFile =
        File(
            artDirectory,
            "track_$trackId.jpg"
        )

    if (
        artFile.exists() &&
        artFile.length() > 0L
    ) {
        return artFile.absolutePath
    }

    if (
        artFile.exists()
    ) {
        artFile.delete()
    }

    val audioUri =
        Uri.parse(
            audioUriString
        )

    val retriever =
        MediaMetadataRetriever()

    return try {
        context.contentResolver.openFileDescriptor(
            audioUri,
            "r"
        )?.use { parcelFileDescriptor ->
            retriever.setDataSource(
                parcelFileDescriptor.fileDescriptor
            )

            val pictureBytes = retriever.embeddedPicture

            if (
                pictureBytes == null || pictureBytes.isEmpty()
            ) {
                Log.d(
                    "AuralArc",
                    "No embedded album art found for $audioUriString"
                )

                return@use null
            }

            val bitmap =
                BitmapFactory.decodeByteArray(
                    pictureBytes,
                    0,
                    pictureBytes.size
                )

            if (
                bitmap == null
            ) {
                Log.d(
                    "AuralArc",
                    "Embedded album art bytes could not be decoded for $audioUriString"
                )

                return@use null
            }

            FileOutputStream(
                artFile
            ).use { outputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    92,
                    outputStream
                )
            }

            Log.d(
                "AuralArc",
                "Embedded album art saved: ${artFile.absolutePath}"
            )

            artFile.absolutePath
        }
    } catch (e: Exception) {
        Log.e(
            "AuralArc",
            "Failed to extract embedded album art for $audioUriString",
            e
        )

        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

fun clearCachedEmbeddedAlbumArt(
    context: Context,
    trackId: Long
) {
    val artFile =
        File(
            File(
                context.cacheDir,
                "album_art"
            ),
            "track_$trackId.jpg"
        )

    try {
        if (
            artFile.exists()
        ) {
            artFile.delete()
        }
    } catch (_: Exception) {
    }
}

fun albumArtUri(
    albumId: Long
): Uri? {
    return null
}