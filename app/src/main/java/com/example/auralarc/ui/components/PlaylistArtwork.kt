package com.example.auralarc.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.data.Playlist
import com.example.auralarc.storage.PlaylistArtworkStore
import com.example.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private object PlaylistArtworkBitmapCache {

    private val cache =
        object : LruCache<String, ImageBitmap>(
            120
        ) {}

    fun get(
        artworkPath: String
    ): ImageBitmap? {
        return cache.get(
            artworkPath
        )
    }

    fun put(
        artworkPath: String,
        bitmap: ImageBitmap
    ) {
        cache.put(
            artworkPath,
            bitmap
        )
    }
}

@Composable
fun PlaylistArtwork(
    playlist: Playlist,
    tracks: List<MusicTrack>,
    size: Dp,
    refreshKey: Int = 0
) {
    val context =
        LocalContext.current

    /*
     * Reading this preference during recomposition is inexpensive.
     * refreshKey forces an immediate refresh after the user changes
     * or removes custom artwork.
     */
    val customArtworkPath =
        remember(
            playlist.id,
            playlist.source,
            refreshKey
        ) {
            PlaylistArtworkStore.getCustomArtworkPath(
                context = context,
                playlist = playlist
            )
        }

    val automaticArtworkPaths =
        remember(
            tracks,
            customArtworkPath
        ) {
            tracks
                .asSequence()
                .filter { track ->
                    !track.albumArtPath.isNullOrBlank()
                }
                .distinctBy { track ->
                    /*
                     * Prefer the real album ID when one exists.
                     * Otherwise, identify the album using its name
                     * and album artist.
                     */
                    if (
                        track.albumId > 0L
                    ) {
                        "album_id:${track.albumId}"
                    } else {
                        val albumArtist =
                            track.albumArtist
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?: track.artist

                        "album_metadata:" +
                                track.album
                                    .trim()
                                    .lowercase() +
                                "|" +
                                albumArtist
                                    .trim()
                                    .lowercase()
                    }
                }
                .mapNotNull { track ->
                    track.albumArtPath
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                }
                /*
                 * This second duplicate check handles separate
                 * albums that happen to return the exact same
                 * artwork path.
                 */
                .distinct()
                .take(
                    4
                )
                .toList()
        }
    val artworkPaths =
        if (
            !customArtworkPath.isNullOrBlank()
        ) {
            listOf(
                customArtworkPath
            )
        } else {
            automaticArtworkPaths
        }

    Card(
        modifier = Modifier.size(
            size
        ),
        shape = AuralArcStyle.ArtworkShape,
        backgroundColor = AuralArcStyle.SurfaceSoft,
        elevation = 4.dp
    ) {
        when (
            artworkPaths.size
        ) {
            0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            AuralArcStyle.SurfaceSoft
                        )
                ) {
                    Icon(
                        imageVector =
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Playlist artwork",
                        tint = AuralArcStyle.PurpleBright,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                size / 4f
                            )
                    )
                }
            }

            1 -> {
                PlaylistArtworkImage(
                    artworkPath = artworkPaths[0],
                    modifier = Modifier.fillMaxSize()
                )
            }

            2 -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PlaylistArtworkImage(
                        artworkPath = artworkPaths[0],
                        modifier = Modifier
                            .weight(
                                1f
                            )
                            .fillMaxHeight()
                    )

                    PlaylistArtworkImage(
                        artworkPath = artworkPaths[1],
                        modifier = Modifier
                            .weight(
                                1f
                            )
                            .fillMaxHeight()
                    )
                }
            }

            3 -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PlaylistArtworkImage(
                        artworkPath = artworkPaths[0],
                        modifier = Modifier
                            .weight(
                                1f
                            )
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .weight(
                                1f
                            )
                            .fillMaxHeight()
                    ) {
                        PlaylistArtworkImage(
                            artworkPath = artworkPaths[1],
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .fillMaxWidth()
                        )

                        PlaylistArtworkImage(
                            artworkPath = artworkPaths[2],
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .weight(
                                1f
                            )
                            .fillMaxWidth()
                    ) {
                        PlaylistArtworkImage(
                            artworkPath = artworkPaths[0],
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .fillMaxHeight()
                        )

                        PlaylistArtworkImage(
                            artworkPath = artworkPaths[1],
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .fillMaxHeight()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(
                                1f
                            )
                            .fillMaxWidth()
                    ) {
                        PlaylistArtworkImage(
                            artworkPath = artworkPaths[2],
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .fillMaxHeight()
                        )

                        PlaylistArtworkImage(
                            artworkPath = artworkPaths[3],
                            modifier = Modifier
                                .weight(
                                    1f
                                )
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistArtworkImage(
    artworkPath: String,
    modifier: Modifier
) {
    val context =
        LocalContext.current

    var imageBitmap by remember(
        artworkPath
    ) {
        mutableStateOf(
            PlaylistArtworkBitmapCache.get(
                artworkPath
            )
        )
    }

    LaunchedEffect(
        artworkPath
    ) {
        val cachedBitmap =
            PlaylistArtworkBitmapCache.get(
                artworkPath
            )

        if (
            cachedBitmap != null
        ) {
            imageBitmap =
                cachedBitmap

            return@LaunchedEffect
        }

        val loadedBitmap =
            withContext(
                Dispatchers.IO
            ) {
                loadPlaylistArtworkBitmap(
                    context = context.applicationContext,
                    artworkPath = artworkPath
                )
            }

        if (
            loadedBitmap != null
        ) {
            PlaylistArtworkBitmapCache.put(
                artworkPath = artworkPath,
                bitmap = loadedBitmap
            )
        }

        imageBitmap =
            loadedBitmap
    }

    Box(
        modifier = modifier
            .background(
                AuralArcStyle.SurfaceSoft
            )
    ) {
        val loadedImage =
            imageBitmap

        if (
            loadedImage != null
        ) {
            Image(
                bitmap = loadedImage,
                contentDescription = "Playlist artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun loadPlaylistArtworkBitmap(
    context: android.content.Context,
    artworkPath: String
): ImageBitmap? {
    return try {
        val bitmap =
            when {
                artworkPath.startsWith(
                    "http://"
                ) ||
                        artworkPath.startsWith(
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