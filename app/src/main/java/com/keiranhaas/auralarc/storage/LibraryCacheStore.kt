package com.keiranhaas.auralarc.storage

import android.content.Context
import com.keiranhaas.auralarc.data.LibrarySource
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object LibraryCacheStore {

    private const val CACHE_DIRECTORY_NAME =
        "library_cache"

    private var legacyCacheCleanupCompleted =
        false

    fun loadTracks(
        context: Context,
        source: LibrarySource
    ): List<MusicTrack>? {
        val cachedObject =
            readObject(
                trackCacheFile(
                    context = context,
                    source = source
                )
            ) ?: return null

        val cachedList =
            cachedObject as? List<*>
                ?: return null

        return cachedList.mapNotNull { item ->
            item as? MusicTrack
        }
    }

    fun saveTracks(
        context: Context,
        source: LibrarySource,
        tracks: List<MusicTrack>
    ) {
        writeObject(
            targetFile =
            trackCacheFile(
                context = context,
                source = source
            ),
            value =
            ArrayList(
                tracks
            )
        )
    }

    fun clearTracks(
        context: Context,
        source: LibrarySource
    ) {
        try {
            trackCacheFile(
                context = context,
                source = source
            ).delete()
        } catch (_: Exception) {
        }
    }

    fun loadNavidromePlaylists(
        context: Context
    ): List<Playlist>? {
        val cachedObject =
            readObject(
                navidromePlaylistCacheFile(
                    context
                )
            ) ?: return null

        val cachedList =
            cachedObject as? List<*>
                ?: return null

        return cachedList.mapNotNull { item ->
            item as? Playlist
        }
    }

    fun saveNavidromePlaylists(
        context: Context,
        playlists: List<Playlist>
    ) {
        writeObject(
            targetFile =
            navidromePlaylistCacheFile(
                context
            ),
            value =
            ArrayList(
                playlists
            )
        )
    }

    fun clearNavidromePlaylists(
        context: Context
    ) {
        try {
            navidromePlaylistCacheFile(
                context
            ).delete()
        } catch (_: Exception) {
        }
    }

    private fun trackCacheFile(
        context: Context,
        source: LibrarySource
    ): File {
        return File(
            cacheDirectory(
                context
            ),
            "tracks_${source.name.lowercase()}.bin.gz"
        )
    }

    private fun navidromePlaylistCacheFile(
        context: Context
    ): File {
        return File(
            cacheDirectory(
                context
            ),
            "navidrome_playlists.bin.gz"
        )
    }

    private fun cacheDirectory(
        context: Context
    ): File {
        val directory =
            File(
                context.applicationContext.filesDir,
                CACHE_DIRECTORY_NAME
            )

        if (
            !directory.exists()
        ) {
            directory.mkdirs()
        }

        if (
            !legacyCacheCleanupCompleted
        ) {
            legacyCacheCleanupCompleted =
                true

            deleteLegacyVersionedCaches(
                directory
            )
        }

        return directory
    }

    private fun deleteLegacyVersionedCaches(
        directory: File
    ) {
        try {
            directory
                .listFiles()
                ?.filter { file ->
                    file.name.startsWith(
                        "tracks_v"
                    ) ||
                            file.name.startsWith(
                                "navidrome_playlists_v"
                            )
                }
                ?.forEach { file ->
                    try {
                        file.delete()
                    } catch (_: Exception) {
                    }
                }
        } catch (_: Exception) {
        }
    }

    private fun readObject(
        sourceFile: File
    ): Any? {
        if (
            !sourceFile.exists() ||
            sourceFile.length() <= 0L
        ) {
            return null
        }

        return try {
            FileInputStream(
                sourceFile
            ).use { fileInput ->
                BufferedInputStream(
                    fileInput
                ).use { bufferedInput ->
                    GZIPInputStream(
                        bufferedInput
                    ).use { gzipInput ->
                        ObjectInputStream(
                            gzipInput
                        ).use { objectInput ->
                            objectInput.readObject()
                        }
                    }
                }
            }
        } catch (_: Exception) {
            /*
             * A model update can make an older serialized cache
             * incompatible. Delete it and allow a fresh scan to
             * recreate the cache.
             */
            try {
                sourceFile.delete()
            } catch (_: Exception) {
            }

            null
        }
    }

    private fun writeObject(
        targetFile: File,
        value: Serializable
    ) {
        val temporaryFile =
            File(
                targetFile.parentFile,
                "${targetFile.name}.tmp"
            )

        try {
            FileOutputStream(
                temporaryFile
            ).use { fileOutput ->
                BufferedOutputStream(
                    fileOutput
                ).use { bufferedOutput ->
                    GZIPOutputStream(
                        bufferedOutput
                    ).use { gzipOutput ->
                        ObjectOutputStream(
                            gzipOutput
                        ).use { objectOutput ->
                            objectOutput.writeObject(
                                value
                            )

                            objectOutput.flush()
                        }
                    }
                }
            }

            if (
                targetFile.exists()
            ) {
                targetFile.delete()
            }

            val renamed =
                temporaryFile.renameTo(
                    targetFile
                )

            if (
                !renamed
            ) {
                temporaryFile.copyTo(
                    target = targetFile,
                    overwrite = true
                )

                temporaryFile.delete()
            }
        } catch (_: Exception) {
            try {
                temporaryFile.delete()
            } catch (_: Exception) {
            }
        }
    }
}