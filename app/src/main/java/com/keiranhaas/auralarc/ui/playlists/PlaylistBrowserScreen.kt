package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.LibrarySource
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import com.keiranhaas.auralarc.navidrome.NavidromePlaylistManager
import com.keiranhaas.auralarc.storage.LibraryCacheStore
import com.keiranhaas.auralarc.storage.LikedSongsStore
import com.keiranhaas.auralarc.storage.PlaylistArtworkStore
import com.keiranhaas.auralarc.storage.PlaylistStore
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
fun PlaylistBrowserScreen(
    allTracks: List<MusicTrack>,
    librarySource: LibrarySource,
    onPlaylistSelected: (String) -> Unit,
    viewMode: LibraryViewMode = LibraryViewMode.LIST
) {
    val context =
        LocalContext.current

    var playlists by remember(
        librarySource
    ) {
        mutableStateOf<List<Playlist>>(
            emptyList()
        )
    }

    var isLoading by remember(
        librarySource
    ) {
        mutableStateOf(
            true
        )
    }

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var playlistToDelete by remember {
        mutableStateOf<Playlist?>(null)
    }

    val coroutineScope =
        rememberCoroutineScope()

    var editMessage by remember {
        mutableStateOf<String?>(null)
    }

    suspend fun reloadPlaylists() {
        isLoading =
            true

        when (
            librarySource
        ) {
            LibrarySource.LOCAL -> {
                playlists =
                    withContext(
                        Dispatchers.IO
                    ) {
                        PlaylistStore.getPlaylists(
                            context
                        )
                    }

                isLoading =
                    false
            }

            LibrarySource.NAVIDROME -> {
                /*
                 * Display the previously loaded server playlists
                 * while the current request runs.
                 */
                val cachedPlaylists =
                    withContext(
                        Dispatchers.IO
                    ) {
                        LibraryCacheStore.loadNavidromePlaylists(
                            context
                        )
                    }

                if (
                    cachedPlaylists != null
                ) {
                    playlists =
                        cachedPlaylists
                }

                val refreshedPlaylists =
                    withContext(
                        Dispatchers.IO
                    ) {
                        NavidromePlaylistManager.loadPlaylistsOrNull(
                            context
                        )
                    }

                if (
                    refreshedPlaylists != null
                ) {
                    playlists =
                        refreshedPlaylists

                    withContext(
                        Dispatchers.IO
                    ) {
                        LibraryCacheStore.saveNavidromePlaylists(
                            context = context,
                            playlists = refreshedPlaylists
                        )
                    }
                }

                isLoading =
                    false
            }
        }
    }

    LaunchedEffect(
        librarySource
    ) {
        reloadPlaylists()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PlaylistBrowserHeader(
            playlistCount = playlists.size + 1,
            librarySource = librarySource,
            isLoading = isLoading,
            onCreateClick = {
                showCreateDialog =
                    true
            }
        )

        when {
            isLoading &&
                    playlists.isEmpty() -> {
                EmptyPlaylistBrowser(
                    title = "Loading playlists...",
                    message = "Please wait while AuralArc loads ${librarySource.name.lowercase()} playlists."
                )
            }

            viewMode == LibraryViewMode.GRID -> {
                PlaylistGrid(
                    playlists = playlists,
                    allTracks = allTracks,
                    librarySource = librarySource,
                    onPlaylistSelected = onPlaylistSelected
                )
            }

            else -> {
                val likedTracks =
                    remember(
                        allTracks,
                        LikedSongsStore.revision.value
                    ) {
                        LikedSongsStore.getLikedTracks(
                            context = context,
                            allTracks = allTracks
                        )
                    }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 10.dp
                    )
                ) {
                    item(
                        key = LikedSongsStore.PLAYLIST_ID
                    ) {
                        PlaylistBrowserRow(
                            playlist = LikedSongsStore.asPlaylist(
                                trackCount = likedTracks.size
                            ),
                            tracks = likedTracks,
                            trackCount = likedTracks.size,
                            canDelete = false,
                            onClick = {
                                onPlaylistSelected(
                                    LikedSongsStore.PLAYLIST_ID
                                )
                            },
                            onDelete = {}
                        )
                    }

                    if (
                        playlists.isEmpty()
                    ) {
                        item {
                            EmptyPlaylistBrowser(
                                title =
                                    if (
                                        librarySource == LibrarySource.NAVIDROME
                                    ) {
                                        "No Navidrome playlists"
                                    } else {
                                        "No other playlists yet"
                                    },
                                message =
                                    if (
                                        librarySource == LibrarySource.NAVIDROME
                                    ) {
                                        "Tap + above to create a playlist on your Navidrome server."
                                    } else {
                                        "Create a playlist, then add songs from your library."
                                    }
                            )
                        }
                    }

                    items(
                        playlists
                    ) { playlist ->
                        val playlistTracks =
                            if (
                                librarySource == LibrarySource.LOCAL
                            ) {
                                PlaylistStore.getTracksForPlaylist(
                                    playlist = playlist,
                                    allTracks = allTracks
                                )
                            } else {
                                emptyList()
                            }

                        val trackCount =
                            if (
                                librarySource == LibrarySource.NAVIDROME
                            ) {
                                playlist.trackCount
                            } else {
                                playlistTracks.size
                            }

                        PlaylistBrowserRow(
                            playlist = playlist,
                            tracks = playlistTracks,
                            trackCount = trackCount,
                            canDelete = true,
                            onClick = {
                                onPlaylistSelected(
                                    playlist.id
                                )
                            },
                            onDelete = {
                                playlistToDelete =
                                    playlist
                            }
                        )
                    }
                }
            }
        }
    }

    if (
        showCreateDialog
    ) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreateDialog =
                    false
            },
            onCreate = { playlistName ->
                showCreateDialog =
                    false

                if (
                    librarySource == LibrarySource.LOCAL
                ) {
                    PlaylistStore.createPlaylist(
                        context = context,
                        rawName = playlistName
                    )

                    playlists =
                        PlaylistStore.getPlaylists(
                            context
                        )
                } else {
                    coroutineScope.launch {
                        val result =
                            withContext(
                                Dispatchers.IO
                            ) {
                                NavidromePlaylistManager.createPlaylist(
                                    context = context,
                                    name = playlistName
                                )
                            }

                        if (
                            result.success
                        ) {
                            reloadPlaylists()
                        } else {
                            editMessage =
                                result.message
                        }
                    }
                }
            }
        )
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlist = playlist,
            onDismiss = {
                playlistToDelete =
                    null
            },
            onDelete = {
                playlistToDelete =
                    null

                if (
                    librarySource == LibrarySource.LOCAL
                ) {
                    PlaylistArtworkStore.removeCustomArtwork(
                        context = context,
                        playlist = playlist
                    )

                    PlaylistStore.deletePlaylist(
                        context = context,
                        playlistId = playlist.id
                    )

                    playlists =
                        PlaylistStore.getPlaylists(
                            context
                        )
                } else {
                    coroutineScope.launch {
                        val result =
                            withContext(
                                Dispatchers.IO
                            ) {
                                NavidromePlaylistManager.deletePlaylist(
                                    context = context,
                                    playlistId = playlist.id
                                )
                            }

                        if (
                            result.success
                        ) {
                            PlaylistArtworkStore.removeCustomArtwork(
                                context = context,
                                playlist = playlist
                            )

                            reloadPlaylists()
                        } else {
                            editMessage =
                                result.message
                        }
                    }
                }
            }
        )
    }

    editMessage?.let { message ->
        AlertDialog(
            containerColor = AuralArcStyle.Surface,
            onDismissRequest = {
                editMessage =
                    null
            },
            title = {
                Text(
                    text = "Navidrome Playlist"
                )
            },
            text = {
                Text(
                    text = message
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editMessage =
                            null
                    }
                ) {
                    Text(
                        text = "OK"
                    )
                }
            }
        )
    }
}

@Composable
private fun PlaylistBrowserHeader(
    playlistCount: Int,
    librarySource: LibrarySource,
    isLoading: Boolean,
    onCreateClick: () -> Unit
) {
    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Playlists",
                tint = AuralArcStyle.PurpleBright,
                modifier = Modifier.size(
                    34.dp
                )
            )

            Spacer(
                modifier = Modifier.width(
                    12.dp
                )
            )

            Column(
                modifier = Modifier
                    .weight(
                        1f
                    )
            ) {
                Text(
                    text =
                    if (
                        librarySource == LibrarySource.NAVIDROME
                    ) {
                        "Navidrome Playlists"
                    } else {
                        "Local Playlists"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text =
                    if (
                        isLoading
                    ) {
                        "Loading..."
                    } else if (
                        playlistCount == 1
                    ) {
                        "1 playlist"
                    } else {
                        "$playlistCount playlists"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AuralArcStyle.TextMuted
                )
            }

            if (
                librarySource == LibrarySource.LOCAL ||
                librarySource == LibrarySource.NAVIDROME
            ) {
                AuralArcCard(
                    modifier = Modifier.size(
                        44.dp
                    ),
                    shape =
                    AuralArcStyle.SmallShape,
                    backgroundColor =
                    AuralArcStyle.PurpleDark,
                    elevation = 6.dp
                ) {
                    AuralArcIconButton(
                        onClick = onCreateClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector =
                            Icons.Default.Add,
                            contentDescription =
                            "Create playlist",
                            tint =
                            AuralArcStyle.TextPrimary,
                            modifier = Modifier.size(
                                24.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistBrowserRow(
    playlist: Playlist,
    tracks: List<MusicTrack>,
    trackCount: Int,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val sourceLabel =
        when {
            playlist.id == LikedSongsStore.PLAYLIST_ID ->
                "Automatic"

            playlist.source == "NAVIDROME" ->
                "Navidrome"

            else ->
                "Local"
        }

    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistArtwork(
                playlist = playlist,
                tracks = tracks,
                size = 58.dp
            )

            Spacer(
                modifier = Modifier.width(
                    12.dp
                )
            )

            Column(
                modifier = Modifier
                    .weight(
                        1f
                    )
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text =
                    if (
                        trackCount == 1
                    ) {
                        "1 song • $sourceLabel"
                    } else {
                        "$trackCount songs • $sourceLabel"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuralArcStyle.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (
                canDelete
            ) {
                AuralArcIconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete playlist",
                        tint = AuralArcStyle.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistBrowser(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = AuralArcStyle.TextPrimary
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AuralArcStyle.TextMuted
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AuralArcStyle.Surface,
        title = {
            Text(
                text = "Create Playlist"
            )
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = {
                    playlistName =
                        it
                },
                label = {
                    Text(
                        text = "Playlist name"
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled =
                playlistName.isNotBlank(),
                onClick = {
                    onCreate(
                        playlistName
                    )
                }
            ) {
                Text(
                    text = "Create"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel"
                )
            }
        }
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AuralArcStyle.Surface,
        title = {
            Text(
                text = "Delete Playlist"
            )
        },
        text = {
            Text(
                text = "Delete \"${playlist.name}\"? This will not delete any audio files."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDelete
            ) {
                Text(
                    text = "Delete"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel"
                )
            }
        }
    )
}

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    allTracks: List<MusicTrack>,
    librarySource: LibrarySource,
    onPlaylistSelected: (String) -> Unit
) {
    val context =
        LocalContext.current

    val likedTracks =
        remember(
            allTracks,
            LikedSongsStore.revision.value
        ) {
            LikedSongsStore.getLikedTracks(
                context = context,
                allTracks = allTracks
            )
        }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(
            minSize = 112.dp
        ),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 6.dp,
            bottom = 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(
            12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(
            8.dp
        )
    ) {
        item(
            key = LikedSongsStore.PLAYLIST_ID
        ) {
            LibraryGridTile(
                title = LikedSongsStore.PLAYLIST_NAME,
                subtitle =
                    if (
                        likedTracks.size == 1
                    ) {
                        "Playlist • 1 song"
                    } else {
                        "Playlist • ${likedTracks.size} songs"
                    },
                onClick = {
                    onPlaylistSelected(
                        LikedSongsStore.PLAYLIST_ID
                    )
                }
            ) { artSize ->
                PlaylistArtwork(
                    playlist = LikedSongsStore.asPlaylist(
                        trackCount = likedTracks.size
                    ),
                    tracks = likedTracks,
                    size = artSize
                )
            }
        }

        if (
            playlists.isEmpty()
        ) {
            item(
                span = {
                    GridItemSpan(
                        maxLineSpan
                    )
                }
            ) {
                EmptyPlaylistBrowser(
                    title =
                        if (
                            librarySource == LibrarySource.NAVIDROME
                        ) {
                            "No Navidrome playlists"
                        } else {
                            "No other playlists yet"
                        },
                    message =
                        if (
                            librarySource == LibrarySource.NAVIDROME
                        ) {
                            "Tap + above to create a playlist on your Navidrome server."
                        } else {
                            "Create a playlist, then add songs from your library."
                        }
                )
            }
        }

        gridItems(
            playlists
        ) { playlist ->
            val playlistTracks =
                remember(
                    playlist,
                    allTracks,
                    librarySource
                ) {
                    if (
                        librarySource == LibrarySource.LOCAL
                    ) {
                        PlaylistStore.getTracksForPlaylist(
                            playlist = playlist,
                            allTracks = allTracks
                        )
                    } else {
                        emptyList()
                    }
                }

            val trackCount =
                if (
                    librarySource == LibrarySource.NAVIDROME
                ) {
                    playlist.trackCount
                } else {
                    playlistTracks.size
                }

            LibraryGridTile(
                title = playlist.name,
                subtitle =
                    if (
                        trackCount == 1
                    ) {
                        "Playlist • 1 song"
                    } else {
                        "Playlist • $trackCount songs"
                    },
                onClick = {
                    onPlaylistSelected(
                        playlist.id
                    )
                }
            ) { artSize ->
                PlaylistArtwork(
                    playlist = playlist,
                    tracks = playlistTracks,
                    size = artSize
                )
            }
        }
    }
}