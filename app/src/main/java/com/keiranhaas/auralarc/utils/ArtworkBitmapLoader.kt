package com.keiranhaas.auralarc.utils

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object ArtworkBitmapLoader {

    private const val MAX_MEMORY_CACHE_KB =
        32 * 1024

    private const val MIN_MEMORY_CACHE_KB =
        8 * 1024

    private const val REMOTE_CACHE_DIRECTORY =
        "remote_artwork"

    private const val MAX_REMOTE_ARTWORK_BYTES =
        20L * 1024L * 1024L

    private const val MAX_REMOTE_CACHE_BYTES =
        64L * 1024L * 1024L

    private const val REMOTE_CACHE_MAX_AGE_MS =
        30L * 24L * 60L * 60L * 1000L

    private const val REMOTE_CACHE_PRUNE_INTERVAL_MS =
        10L * 60L * 1000L

    private val loaderScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )

    private val memoryCacheSizeKb =
        minOf(
            (
                    Runtime.getRuntime()
                        .maxMemory() /
                            1024L /
                            8L
                    ).toInt(),
            MAX_MEMORY_CACHE_KB
        ).coerceAtLeast(
            MIN_MEMORY_CACHE_KB
        )

    private val memoryCache =
        object : LruCache<String, Bitmap>(
            memoryCacheSizeKb
        ) {
            override fun sizeOf(
                key: String,
                value: Bitmap
            ): Int {
                return (
                        value.allocationByteCount /
                                1024
                        ).coerceAtLeast(
                        1
                    )
            }
        }

    /*
     * Multiple UI elements may request the same artwork at the
     * same time.
     *
     * Share the decode/download instead of doing duplicate work.
     */
    private val inFlightLoads =
        ConcurrentHashMap<
                String,
                Deferred<Bitmap?>
                >()

    @Volatile
    private var lastRemoteCachePruneElapsed =
        0L

    fun peek(
        artworkPath: String,
        targetSizePx: Int
    ): Bitmap? {
        val cleanPath =
            artworkPath.trim()

        if (
            cleanPath.isBlank()
        ) {
            return null
        }

        return memoryCache.get(
            cacheKey(
                cleanPath,
                targetSizePx
            )
        )
    }

    suspend fun load(
        context: Context,
        artworkPath: String,
        targetSizePx: Int
    ): Bitmap? {
        val cleanPath =
            artworkPath.trim()

        if (
            cleanPath.isBlank()
        ) {
            return null
        }

        val applicationContext =
            context.applicationContext

        val targetBucket =
            targetBucket(
                targetSizePx
            )

        val key =
            cacheKey(
                artworkPath = cleanPath,
                targetSizePx = targetBucket
            )

        memoryCache.get(
            key
        )?.let { cached ->
            return cached
        }

        val existingLoad =
            inFlightLoads[
                key
            ]

        if (
            existingLoad != null
        ) {
            return existingLoad.await()
        }

        val newLoad =
            loaderScope.async(
                start =
                    CoroutineStart.LAZY
            ) {
                try {
                    val bitmap =
                        decodeArtwork(
                            context =
                                applicationContext,
                            artworkPath =
                                cleanPath,
                            targetSizePx =
                                targetBucket
                        )

                    if (
                        bitmap != null
                    ) {
                        memoryCache.put(
                            key,
                            bitmap
                        )
                    }

                    bitmap
                } catch (
                    outOfMemory:
                    OutOfMemoryError
                ) {
                    /*
                     * Never allow oversized/problem artwork to
                     * crash the entire music player.
                     */
                    memoryCache.evictAll()

                    null
                } catch (_: Exception) {
                    null
                }
            }

        val competingLoad =
            inFlightLoads.putIfAbsent(
                key,
                newLoad
            )

        val activeLoad =
            if (
                competingLoad == null
            ) {
                newLoad.apply {
                    start()
                }
            } else {
                newLoad.cancel()

                competingLoad
            }

        return try {
            activeLoad.await()
        } finally {
            if (
                competingLoad == null
            ) {
                inFlightLoads.remove(
                    key,
                    newLoad
                )
            }
        }
    }

    fun trimMemory(
        level: Int
    ) {
        when {
            level ==
                    ComponentCallbacks2
                        .TRIM_MEMORY_RUNNING_CRITICAL -> {
                memoryCache.evictAll()
            }

            level >=
                    ComponentCallbacks2
                        .TRIM_MEMORY_BACKGROUND -> {
                memoryCache.trimToSize(
                    memoryCacheSizeKb /
                            4
                )
            }

            level >=
                    ComponentCallbacks2
                        .TRIM_MEMORY_RUNNING_LOW -> {
                memoryCache.trimToSize(
                    memoryCacheSizeKb /
                            2
                )
            }
        }
    }

    fun clearMemory() {
        memoryCache.evictAll()
    }

    private fun cacheKey(
        artworkPath: String,
        targetSizePx: Int
    ): String {
        return "${targetBucket(targetSizePx)}|$artworkPath"
    }

    /*
     * Deliberately reuse a small set of bitmap sizes.
     *
     * Otherwise a 54dp, 56dp and 60dp request could create
     * three separate cached copies of practically identical art.
     */
    private fun targetBucket(
        requestedPx: Int
    ): Int {
        return when {
            requestedPx <= 96 ->
                128

            requestedPx <= 192 ->
                256

            requestedPx <= 384 ->
                512

            else ->
                1024
        }
    }

    private fun decodeArtwork(
        context: Context,
        artworkPath: String,
        targetSizePx: Int
    ): Bitmap? {
        return when {
            artworkPath.startsWith(
                "http://",
                ignoreCase = true
            ) ||
                    artworkPath.startsWith(
                        "https://",
                        ignoreCase = true
                    ) -> {
                decodeRemoteArtwork(
                    context = context,
                    artworkUrl = artworkPath,
                    targetSizePx =
                        targetSizePx
                )
            }

            artworkPath.startsWith(
                "content://",
                ignoreCase = true
            ) -> {
                decodeContentArtwork(
                    context = context,
                    uri = Uri.parse(
                        artworkPath
                    ),
                    targetSizePx =
                        targetSizePx
                )
            }

            artworkPath.startsWith(
                "file://",
                ignoreCase = true
            ) -> {
                val path =
                    Uri.parse(
                        artworkPath
                    ).path
                        ?: return null

                decodeFileArtwork(
                    file = File(
                        path
                    ),
                    targetSizePx =
                        targetSizePx
                )
            }

            else -> {
                decodeFileArtwork(
                    file = File(
                        artworkPath
                    ),
                    targetSizePx =
                        targetSizePx
                )
            }
        }
    }

    private fun decodeFileArtwork(
        file: File,
        targetSizePx: Int
    ): Bitmap? {
        if (
            !file.exists() ||
            file.length() <= 0L
        ) {
            return null
        }

        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds =
                    true
            }

        BitmapFactory.decodeFile(
            file.absolutePath,
            bounds
        )

        if (
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0
        ) {
            return null
        }

        val options =
            BitmapFactory.Options().apply {
                inSampleSize =
                    calculateSampleSize(
                        width =
                            bounds.outWidth,
                        height =
                            bounds.outHeight,
                        targetSizePx =
                            targetSizePx
                    )

                inPreferredConfig =
                    Bitmap.Config.ARGB_8888
            }

        return BitmapFactory.decodeFile(
            file.absolutePath,
            options
        )
    }

    private fun decodeContentArtwork(
        context: Context,
        uri: Uri,
        targetSizePx: Int
    ): Bitmap? {
        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds =
                    true
            }

        context.contentResolver
            .openInputStream(
                uri
            )
            ?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    bounds
                )
            }

        if (
            bounds.outWidth <= 0 ||
            bounds.outHeight <= 0
        ) {
            return null
        }

        val options =
            BitmapFactory.Options().apply {
                inSampleSize =
                    calculateSampleSize(
                        width =
                            bounds.outWidth,
                        height =
                            bounds.outHeight,
                        targetSizePx =
                            targetSizePx
                    )

                inPreferredConfig =
                    Bitmap.Config.ARGB_8888
            }

        return context.contentResolver
            .openInputStream(
                uri
            )
            ?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    options
                )
            }
    }

    private fun decodeRemoteArtwork(
        context: Context,
        artworkUrl: String,
        targetSizePx: Int
    ): Bitmap? {
        val cachedFile =
            remoteArtworkCacheFile(
                context = context,
                artworkUrl = artworkUrl
            )

        if (
            cachedFile.exists() &&
            cachedFile.length() > 0L
        ) {
            cachedFile.setLastModified(
                System.currentTimeMillis()
            )

            return decodeFileArtwork(
                file = cachedFile,
                targetSizePx =
                    targetSizePx
            )
        }

        val downloaded =
            downloadRemoteArtwork(
                artworkUrl = artworkUrl,
                destinationFile =
                    cachedFile
            )

        if (
            !downloaded
        ) {
            return null
        }

        pruneRemoteCacheIfNeeded(
            context
        )

        return decodeFileArtwork(
            file = cachedFile,
            targetSizePx =
                targetSizePx
        )
    }

    private fun downloadRemoteArtwork(
        artworkUrl: String,
        destinationFile: File
    ): Boolean {
        val parent =
            destinationFile.parentFile
                ?: return false

        if (
            !parent.exists()
        ) {
            parent.mkdirs()
        }

        val temporaryFile =
            File(
                parent,
                "${destinationFile.name}.${Thread.currentThread().id}.tmp"
            )

        val connection =
            try {
                URL(
                    artworkUrl
                ).openConnection() as
                        HttpURLConnection
            } catch (_: Exception) {
                return false
            }

        return try {
            connection.connectTimeout =
                15_000

            connection.readTimeout =
                20_000

            connection.instanceFollowRedirects =
                true

            connection.setRequestProperty(
                "User-Agent",
                "AuralArc"
            )

            connection.connect()

            if (
                connection.responseCode !in
                200..299
            ) {
                return false
            }

            val reportedLength =
                connection.contentLengthLong

            if (
                reportedLength >
                MAX_REMOTE_ARTWORK_BYTES
            ) {
                return false
            }

            var totalBytes =
                0L

            connection.inputStream
                .buffered(
                    32 * 1024
                )
                .use { input ->
                    FileOutputStream(
                        temporaryFile
                    )
                        .buffered(
                            32 * 1024
                        )
                        .use { output ->
                            val buffer =
                                ByteArray(
                                    32 * 1024
                                )

                            while (
                                true
                            ) {
                                val read =
                                    input.read(
                                        buffer
                                    )

                                if (
                                    read <= 0
                                ) {
                                    break
                                }

                                totalBytes +=
                                    read

                                if (
                                    totalBytes >
                                    MAX_REMOTE_ARTWORK_BYTES
                                ) {
                                    return false
                                }

                                output.write(
                                    buffer,
                                    0,
                                    read
                                )
                            }

                            output.flush()
                        }
                }

            if (
                temporaryFile.length() <=
                0L
            ) {
                return false
            }

            /*
             * Another request may have completed the same
             * download while this one was running.
             */
            if (
                destinationFile.exists() &&
                destinationFile.length() >
                0L
            ) {
                temporaryFile.delete()

                return true
            }

            if (
                !temporaryFile.renameTo(
                    destinationFile
                )
            ) {
                temporaryFile.copyTo(
                    target =
                        destinationFile,
                    overwrite = true
                )

                temporaryFile.delete()
            }

            destinationFile.length() >
                    0L
        } catch (_: Exception) {
            false
        } finally {
            try {
                temporaryFile.delete()
            } catch (_: Exception) {
            }

            connection.disconnect()
        }
    }

    private fun remoteArtworkCacheFile(
        context: Context,
        artworkUrl: String
    ): File {
        val directory =
            File(
                context.cacheDir,
                REMOTE_CACHE_DIRECTORY
            )

        if (
            !directory.exists()
        ) {
            directory.mkdirs()
        }

        return File(
            directory,
            "${sha256(artworkUrl)}.art"
        )
    }

    private fun pruneRemoteCacheIfNeeded(
        context: Context
    ) {
        val nowElapsed =
            SystemClock.elapsedRealtime()

        if (
            nowElapsed -
            lastRemoteCachePruneElapsed <
            REMOTE_CACHE_PRUNE_INTERVAL_MS
        ) {
            return
        }

        synchronized(
            this
        ) {
            val secondCheck =
                SystemClock.elapsedRealtime()

            if (
                secondCheck -
                lastRemoteCachePruneElapsed <
                REMOTE_CACHE_PRUNE_INTERVAL_MS
            ) {
                return
            }

            lastRemoteCachePruneElapsed =
                secondCheck

            val directory =
                File(
                    context.cacheDir,
                    REMOTE_CACHE_DIRECTORY
                )

            val files =
                directory
                    .listFiles()
                    ?.filter {
                        it.isFile
                    }
                    ?: return

            val now =
                System.currentTimeMillis()

            var totalBytes =
                files.sumOf {
                    it.length()
                }

            files
                .sortedBy {
                    it.lastModified()
                }
                .forEach { file ->
                    val stale =
                        now -
                                file.lastModified() >
                                REMOTE_CACHE_MAX_AGE_MS

                    val overLimit =
                        totalBytes >
                                MAX_REMOTE_CACHE_BYTES

                    if (
                        stale ||
                        overLimit
                    ) {
                        val length =
                            file.length()

                        if (
                            file.delete()
                        ) {
                            totalBytes -=
                                length
                        }
                    }
                }
        }
    }

    private fun calculateSampleSize(
        width: Int,
        height: Int,
        targetSizePx: Int
    ): Int {
        val safeTarget =
            targetSizePx.coerceAtLeast(
                1
            )

        var sampleSize =
            1

        while (
            width /
            (sampleSize * 2) >=
            safeTarget &&
            height /
            (sampleSize * 2) >=
            safeTarget
        ) {
            sampleSize *=
                2
        }

        return sampleSize.coerceAtLeast(
            1
        )
    }

    private fun sha256(
        value: String
    ): String {
        return MessageDigest
            .getInstance(
                "SHA-256"
            )
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString(
                separator = ""
            ) { byte ->
                "%02x".format(
                    byte
                )
            }
    }
}