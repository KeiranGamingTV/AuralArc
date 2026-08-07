package com.example.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.auralarc.data.LibrarySource
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.data.Playlist
import com.example.auralarc.navidrome.NavidromePlaylistManager
import com.example.auralarc.storage.PlaylistStore
import com.example.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.auralarc.storage.PlaylistArtworkStore
import com.example.auralarc.storage.LibraryCacheStore
import androidx.compose.material.icons.filled.Add

@Composable
fun PlaylistBrowserScreen(
    allTracks: List<MusicTrack>,
    librarySource: LibrarySource,
    onPlaylistSelected: (String) -> Unit
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
            playlistCount = playlists.size,
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

            playlists.isEmpty() -> {
                EmptyPlaylistBrowser(
                    title =
                    if (
                        librarySource == LibrarySource.NAVIDROME
                    ) {
                        "No Navidrome playlists"
                    } else {
                        "No playlists yet"
                    },
                    message =
                    if (
                        librarySource == LibrarySource.NAVIDROME
                    ) {
                        "Create playlists in Navidrome, then refresh this screen."
                    } else {
                        "Create a playlist, then add songs from your library."
                    }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 10.dp
                    )
                ) {
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
                            canDelete =
                            librarySource == LibrarySource.LOCAL,
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
        showCreateDialog &&
        librarySource == LibrarySource.LOCAL
    ) {
        CreatePlaylistDialog(
            onDismiss = {
                showCreateDialog =
                    false
            },
            onCreate = { playlistName ->
                PlaylistStore.createPlaylist(
                    context = context,
                    rawName = playlistName
                )

                playlists =
                    PlaylistStore.getPlaylists(
                        context
                    )

                showCreateDialog =
                    false
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

                playlistToDelete =
                    null
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
    Card(
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
                    style = MaterialTheme.typography.h6,
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
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )
            }

            if (
                librarySource == LibrarySource.LOCAL
            ) {
                Card(
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
        if (
            playlist.source == "NAVIDROME"
        ) {
            "Navidrome"
        } else {
            "Local"
        }

    Card(
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
                    style = MaterialTheme.typography.subtitle1,
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
                    style = MaterialTheme.typography.body2,
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
            style = MaterialTheme.typography.h6,
            color = AuralArcStyle.TextPrimary
        )

        Text(
            text = message,
            style = MaterialTheme.typography.body2,
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