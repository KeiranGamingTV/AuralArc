package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import com.keiranhaas.auralarc.data.TrackIdentity
import com.keiranhaas.auralarc.navidrome.NavidromePlaylistManager
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.storage.PlaylistStore
import com.keiranhaas.auralarc.ui.components.menuReveal
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoreOptionsButton(
    track: MusicTrack,
    queueTracks: List<MusicTrack>,
    modifier: Modifier = Modifier,
    showAddAlbumToPlaylist: Boolean = true,
    showAddToQueue: Boolean = true,
    showPlayNext: Boolean = true,
    onOpenTrackInfo: ((MusicTrack) -> Unit)? = null
) {

    val context =
        LocalContext.current

    var expanded by remember {
        mutableStateOf(false)
    }

    var tracksToAdd by remember {
        mutableStateOf<List<MusicTrack>?>(null)
    }

    var addDialogTitle by remember {
        mutableStateOf("Add to Playlist")
    }

    Box(
        modifier = modifier
    ) {
        AuralArcIconButton(
            onClick = {
                expanded =
                    true
            }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = AuralArcStyle.TextPrimary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded =
                    false
            }
        ) {
            DropdownMenuItem(
                modifier = Modifier.menuReveal(0),
                text = { Text(text = "Add Song to Playlist") },
                onClick = {
                    expanded =
                        false

                    addDialogTitle =
                        "Add Song to Playlist"

                    tracksToAdd =
                        listOf(
                            track
                        )
                }
            )

            if (
                showAddAlbumToPlaylist
            ) {
                DropdownMenuItem(
                    modifier = Modifier.menuReveal(1),
                    text = { Text(text = "Add Album to Playlist") },
                    onClick = {
                        expanded =
                            false

                        val selectedAlbumArtist =
                            track.albumArtist
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?: track.artist

                        val albumTracks =
                            queueTracks
                                .filter { candidate ->
                                    val bothHaveAlbumIds =
                                        track.albumId > 0L &&
                                                candidate.albumId > 0L

                                    if (
                                        bothHaveAlbumIds
                                    ) {
                                        candidate.albumId ==
                                                track.albumId
                                    } else {
                                        val candidateAlbumArtist =
                                            candidate.albumArtist
                                                .takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?: candidate.artist

                                        candidate.album.equals(
                                            track.album,
                                            ignoreCase = true
                                        ) &&
                                                candidateAlbumArtist.equals(
                                                    selectedAlbumArtist,
                                                    ignoreCase = true
                                                )
                                    }
                                }
                                .sortedWith(
                                    compareBy<MusicTrack> { albumTrack ->
                                        if (
                                            albumTrack.discNumber > 0
                                        ) {
                                            albumTrack.discNumber
                                        } else {
                                            Int.MAX_VALUE
                                        }
                                    }.thenBy { albumTrack ->
                                        if (
                                            albumTrack.trackNumber > 0
                                        ) {
                                            albumTrack.trackNumber
                                        } else {
                                            Int.MAX_VALUE
                                        }
                                    }.thenBy { albumTrack ->
                                        albumTrack.title.lowercase()
                                    }
                                )

                        addDialogTitle =
                            "Add Album to Playlist"

                        tracksToAdd =
                            albumTracks.ifEmpty {
                                listOf(
                                    track
                                )
                            }
                    }
                )
            }

            if (
                showAddToQueue
            ) {
                DropdownMenuItem(
                    modifier = Modifier.menuReveal(2),
                    text = { Text(text = "Add to Queue") },
                    onClick = {
                        expanded =
                            false

                        PlayerManager.addToQueue(
                            context = context,
                            track = track
                        )
                    }
                )
            }

            if (
                showPlayNext
            ) {
                DropdownMenuItem(
                    modifier = Modifier.menuReveal(3),
                    text = { Text(text = "Play Next") },
                    onClick = {
                        expanded =
                            false

                        PlayerManager.playNext(
                            context = context,
                            track = track
                        )
                    }
                )
            }

            DropdownMenuItem(
                modifier = Modifier.menuReveal(4),
                text = { Text(text = "Track Info") },
                onClick = {
                    expanded =
                        false

                    onOpenTrackInfo?.invoke(
                        track
                    )
                }
            )
        }
    }

    tracksToAdd?.let { selectedTracks ->
        AddTracksToPlaylistDialog(
            title = addDialogTitle,
            tracks = selectedTracks,
            onDismiss = {
                tracksToAdd =
                    null
            }
        )
    }
}

@Composable
private fun AddTracksToPlaylistDialog(
    title: String,
    tracks: List<MusicTrack>,
    onDismiss: () -> Unit
) {
    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    val isNavidromeSelection =
        remember(tracks) {
            tracks.isNotEmpty() &&
                    tracks.all { track ->
                        TrackIdentity.navidromeSongId(track) != null
                    }
        }

    var playlists by remember {
        mutableStateOf(
            PlaylistStore.getPlaylists(
                context
            )
        )
    }

    var remotePlaylists by remember {
        mutableStateOf<List<Playlist>>(
            emptyList()
        )
    }

    var remoteLoading by remember {
        mutableStateOf(
            isNavidromeSelection
        )
    }

    var remoteLoadFailed by remember {
        mutableStateOf(false)
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var newPlaylistName by remember {
        mutableStateOf("")
    }

    LaunchedEffect(
        isNavidromeSelection
    ) {
        if (
            isNavidromeSelection
        ) {
            val loaded =
                withContext(
                    Dispatchers.IO
                ) {
                    NavidromePlaylistManager.loadPlaylistsOrNull(
                        context
                    )
                }

            if (
                loaded == null
            ) {
                remoteLoadFailed =
                    true
            } else {
                remotePlaylists =
                    loaded
            }

            remoteLoading =
                false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title
            )
        },
        text = {
            Column {
                Text(
                    text =
                        if (
                            tracks.size == 1
                        ) {
                            "1 song selected"
                        } else {
                            "${tracks.size} songs selected"
                        }
                )

                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = {
                        newPlaylistName =
                            it
                    },
                    label = {
                        Text(
                            text = "New playlist name"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    enabled = newPlaylistName.isNotBlank(),
                    onClick = {
                        val playlist =
                            PlaylistStore.createPlaylist(
                                context = context,
                                rawName = newPlaylistName
                            )

                        PlaylistStore.addTracksToPlaylist(
                            context = context,
                            playlistId = playlist.id,
                            tracks = tracks
                        )

                        playlists =
                            PlaylistStore.getPlaylists(
                                context
                            )

                        newPlaylistName =
                            ""

                        onDismiss()
                    }
                ) {
                    Text(
                        text =
                            if (
                                isNavidromeSelection
                            ) {
                                "Create Local Playlist"
                            } else {
                                "Create and Add"
                            }
                    )
                }

                if (
                    isNavidromeSelection
                ) {
                    TextButton(
                        enabled =
                            newPlaylistName.isNotBlank() &&
                                    !busy,
                        onClick = {
                            busy =
                                true

                            errorMessage =
                                null

                            coroutineScope.launch {
                                val result =
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        NavidromePlaylistManager.createPlaylist(
                                            context = context,
                                            name = newPlaylistName,
                                            tracks = tracks
                                        )
                                    }

                                busy =
                                    false

                                if (
                                    result.success
                                ) {
                                    onDismiss()
                                } else {
                                    errorMessage =
                                        result.message
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Create on Navidrome"
                        )
                    }
                }

                Text(
                    text = "Local playlists",
                    style = MaterialTheme.typography.titleSmall,
                    color = AuralArcStyle.TextSecondary
                )

                LazyColumn(
                    modifier = Modifier.heightIn(
                        max = 160.dp
                    )
                ) {
                    items(
                        playlists
                    ) { playlist ->
                        TextButton(
                            onClick = {
                                PlaylistStore.addTracksToPlaylist(
                                    context = context,
                                    playlistId = playlist.id,
                                    tracks = tracks
                                )

                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = playlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (
                    isNavidromeSelection
                ) {
                    Text(
                        text = "Navidrome playlists",
                        style = MaterialTheme.typography.titleSmall,
                        color = AuralArcStyle.TextSecondary
                    )

                    when {
                        remoteLoading -> {
                            Text(
                                text = "Loading...",
                                color = AuralArcStyle.TextMuted
                            )
                        }

                        remoteLoadFailed -> {
                            Text(
                                text = "Could not load Navidrome playlists.",
                                color = AuralArcStyle.TextMuted
                            )
                        }

                        remotePlaylists.isEmpty() -> {
                            Text(
                                text = "No Navidrome playlists yet.",
                                color = AuralArcStyle.TextMuted
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.heightIn(
                                    max = 160.dp
                                )
                            ) {
                                items(
                                    remotePlaylists
                                ) { playlist ->
                                    TextButton(
                                        enabled = !busy,
                                        onClick = {
                                            busy =
                                                true

                                            errorMessage =
                                                null

                                            coroutineScope.launch {
                                                val result =
                                                    withContext(
                                                        Dispatchers.IO
                                                    ) {
                                                        NavidromePlaylistManager.addTracks(
                                                            context = context,
                                                            playlistId = playlist.id,
                                                            tracks = tracks
                                                        )
                                                    }

                                                busy =
                                                    false

                                                if (
                                                    result.success
                                                ) {
                                                    onDismiss()
                                                } else {
                                                    errorMessage =
                                                        result.message
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = playlist.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Close"
                )
            }
        }
    )
}