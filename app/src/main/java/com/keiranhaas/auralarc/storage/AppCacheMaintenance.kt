package com.keiranhaas.auralarc.storage

import android.content.Context
import java.io.File

object AppCacheMaintenance {

    private const val MAX_ALBUM_ART_CACHE_BYTES =
        96L * 1024L * 1024L

    private const val STALE_ARTWORK_AGE_MS =
        30L * 24L * 60L * 60L * 1000L

    private const val TEMP_FILE_AGE_MS =
        24L * 60L * 60L * 1000L

    fun prune(
        context: Context,
        referencedArtworkPaths: Set<String>
    ) {
        val appContext =
            context.applicationContext

        pruneAlbumArtwork(
            context = appContext,
            referencedArtworkPaths =
                referencedArtworkPaths
        )

        pruneEditorTemporaryFiles(
            appContext
        )
    }

    private fun pruneAlbumArtwork(
        context: Context,
        referencedArtworkPaths: Set<String>
    ) {
        val directory =
            File(
                context.cacheDir,
                "album_art"
            )

        val files =
            directory.listFiles()
                ?.filter { file ->
                    file.isFile
                }
                ?: return

        val now =
            System.currentTimeMillis()

        /*
         * Delete broken files first.
         */
        files
            .filter { file ->
                file.length() <= 0L
            }
            .forEach { file ->
                try {
                    file.delete()
                } catch (_: Exception) {
                }
            }

        var totalBytes =
            files.sumOf { file ->
                file.length()
            }

        val removable =
            files
                .filter { file ->
                    file.absolutePath !in
                            referencedArtworkPaths
                }
                .sortedBy { file ->
                    file.lastModified()
                }

        removable.forEach { file ->
            val stale =
                now -
                        file.lastModified() >=
                        STALE_ARTWORK_AGE_MS

            val overLimit =
                totalBytes >
                        MAX_ALBUM_ART_CACHE_BYTES

            if (
                stale ||
                overLimit
            ) {
                val length =
                    file.length()

                try {
                    if (
                        file.delete()
                    ) {
                        totalBytes -=
                            length
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun pruneEditorTemporaryFiles(
        context: Context
    ) {
        val directory =
            File(
                context.cacheDir,
                "audio_tag_editor"
            )

        val now =
            System.currentTimeMillis()

        directory
            .listFiles()
            ?.forEach { file ->
                if (
                    now -
                    file.lastModified() >=
                    TEMP_FILE_AGE_MS
                ) {
                    try {
                        file.deleteRecursively()
                    } catch (_: Exception) {
                    }
                }
            }
    }
}