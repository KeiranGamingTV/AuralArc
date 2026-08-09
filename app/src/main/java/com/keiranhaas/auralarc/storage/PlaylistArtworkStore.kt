package com.keiranhaas.auralarc.storage

import android.content.Context
import android.net.Uri
import com.keiranhaas.auralarc.data.Playlist
import java.io.File
import java.io.FileOutputStream

object PlaylistArtworkStore {

    private const val PREFERENCES_NAME =
        "playlist_artwork_preferences"

    private const val DIRECTORY_NAME =
        "playlist_artwork"

    private fun preferences(
        context: Context
    ) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun getCustomArtworkPath(
        context: Context,
        playlist: Playlist
    ): String? {
        val preferenceKey =
            artworkPreferenceKey(
                playlist
            )

        val storedPath =
            preferences(
                context
            ).getString(
                preferenceKey,
                null
            )

        if (
            storedPath.isNullOrBlank()
        ) {
            return null
        }

        val storedFile =
            File(
                storedPath
            )

        if (
            !storedFile.exists() ||
            storedFile.length() <= 0L
        ) {
            preferences(
                context
            ).edit()
                .remove(
                    preferenceKey
                )
                .apply()

            return null
        }

        return storedFile.absolutePath
    }

    fun setCustomArtwork(
        context: Context,
        playlist: Playlist,
        sourceUri: Uri
    ): String? {
        val appContext =
            context.applicationContext

        val artworkDirectory =
            File(
                appContext.filesDir,
                DIRECTORY_NAME
            )

        if (
            !artworkDirectory.exists() &&
            !artworkDirectory.mkdirs()
        ) {
            return null
        }

        val contentType =
            appContext.contentResolver
                .getType(
                    sourceUri
                )
                ?.lowercase()
                .orEmpty()

        val extension =
            when {
                contentType.contains(
                    "png"
                ) ->
                    "png"

                contentType.contains(
                    "webp"
                ) ->
                    "webp"

                else ->
                    "jpg"
            }

        val safePlaylistKey =
            safeFileName(
                "${playlist.source}_${playlist.id}"
            )

        val targetFile =
            File(
                artworkDirectory,
                "playlist_${safePlaylistKey}_${System.currentTimeMillis()}.$extension"
            )

        val previousPath =
            getCustomArtworkPath(
                context = appContext,
                playlist = playlist
            )

        return try {
            val inputStream =
                appContext.contentResolver.openInputStream(
                    sourceUri
                ) ?: return null

            inputStream.use { input ->
                FileOutputStream(
                    targetFile
                ).use { output ->
                    input.copyTo(
                        output
                    )
                }
            }

            if (
                !targetFile.exists() ||
                targetFile.length() <= 0L
            ) {
                targetFile.delete()

                return null
            }

            preferences(
                appContext
            ).edit()
                .putString(
                    artworkPreferenceKey(
                        playlist
                    ),
                    targetFile.absolutePath
                )
                .apply()

            if (
                !previousPath.isNullOrBlank() &&
                previousPath != targetFile.absolutePath
            ) {
                try {
                    File(
                        previousPath
                    ).delete()
                } catch (_: Exception) {
                }
            }

            targetFile.absolutePath
        } catch (_: Exception) {
            try {
                targetFile.delete()
            } catch (_: Exception) {
            }

            null
        }
    }

    fun removeCustomArtwork(
        context: Context,
        playlist: Playlist
    ) {
        val storedPath =
            getCustomArtworkPath(
                context = context,
                playlist = playlist
            )

        if (
            !storedPath.isNullOrBlank()
        ) {
            try {
                File(
                    storedPath
                ).delete()
            } catch (_: Exception) {
            }
        }

        preferences(
            context
        ).edit()
            .remove(
                artworkPreferenceKey(
                    playlist
                )
            )
            .apply()
    }

    private fun artworkPreferenceKey(
        playlist: Playlist
    ): String {
        return "artwork_${playlist.source}_${playlist.id}"
    }

    private fun safeFileName(
        rawValue: String
    ): String {
        return rawValue
            .replace(
                Regex(
                    "[^A-Za-z0-9._-]"
                ),
                "_"
            )
            .take(
                140
            )
    }
}