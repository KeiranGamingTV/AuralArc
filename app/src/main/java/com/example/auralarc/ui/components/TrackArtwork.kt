package com.example.auralarc.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private object TrackArtworkCache {

    private val cache =
        object : LruCache<String, ImageBitmap>(
            90
        ) {}

    fun get(
        key: String
    ): ImageBitmap? {
        return cache.get(
            key
        )
    }

    fun put(
        key: String,
        bitmap: ImageBitmap
    ) {
        cache.put(
            key,
            bitmap
        )
    }
}

@Composable
fun TrackArtwork(
    albumArtPath: String?,
    size: Dp
) {
    val context =
        LocalContext.current

    val artworkKey =
        albumArtPath
            ?.trim()
            .orEmpty()

    var imageBitmap by remember(
        artworkKey
    ) {
        mutableStateOf(
            TrackArtworkCache.get(
                artworkKey
            )
        )
    }

    LaunchedEffect(
        artworkKey
    ) {
        if (
            artworkKey.isBlank()
        ) {
            imageBitmap =
                null

            return@LaunchedEffect
        }

        val cached =
            TrackArtworkCache.get(
                artworkKey
            )

        if (
            cached != null
        ) {
            imageBitmap =
                cached

            return@LaunchedEffect
        }

        val loadedBitmap =
            withContext(
                Dispatchers.IO
            ) {
                loadArtworkBitmap(
                    context = context,
                    artworkPath = artworkKey
                )
            }

        if (
            loadedBitmap != null
        ) {
            TrackArtworkCache.put(
                artworkKey,
                loadedBitmap
            )
        }

        imageBitmap =
            loadedBitmap
    }

    Card(
        modifier = Modifier.size(
            size
        ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 4.dp
    ) {
        if (
            imageBitmap != null
        ) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(
                        size
                    )
                    .background(
                        AuralArcStyle.SurfaceBright
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♪",
                    style = MaterialTheme.typography.h4,
                    color = AuralArcStyle.TextMuted
                )
            }
        }
    }
}

private fun loadArtworkBitmap(
    context: android.content.Context,
    artworkPath: String
): ImageBitmap? {
    return try {
        val bitmap =
            when {
                artworkPath.startsWith(
                    "http://"
                ) || artworkPath.startsWith(
                    "https://"
                ) -> {
                    URL(
                        artworkPath
                    ).openStream().use { stream ->
                        BitmapFactory.decodeStream(
                            stream
                        )
                    }
                }

                artworkPath.startsWith(
                    "content://"
                ) -> {
                    context.contentResolver.openInputStream(
                        Uri.parse(
                            artworkPath
                        )
                    )?.use { stream ->
                        BitmapFactory.decodeStream(
                            stream
                        )
                    }
                }

                artworkPath.startsWith(
                    "file://"
                ) -> {
                    val file =
                        File(
                            Uri.parse(
                                artworkPath
                            ).path ?: ""
                        )

                    if (
                        file.exists()
                    ) {
                        BitmapFactory.decodeFile(
                            file.absolutePath
                        )
                    } else {
                        null
                    }
                }

                else -> {
                    val file =
                        File(
                            artworkPath
                        )

                    if (
                        file.exists()
                    ) {
                        BitmapFactory.decodeFile(
                            file.absolutePath
                        )
                    } else {
                        null
                    }
                }
            }

        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}