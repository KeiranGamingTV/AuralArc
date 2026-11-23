package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.storage.PlaylistStore
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import com.keiranhaas.auralarc.ui.components.menuReveal

@Composable
fun MoreOptionsButton(
    track: MusicTrack,
    queueTracks: List<MusicTrack>,
    modifier: Modifier = Modifier,
    showAddAlbumToPlaylist: Boolean = true,
    showAddToQueue: Boolean = true,
    showPlayNext: Boolean = true,
    onOpenLyrics: ((MusicTrack) -> Unit)? = null,
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
                modifier = Modifier.menuReveal(N),
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
                    modifier = Modifier.menuReveal(N),
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
                            if (
                                albumTracks.isNotEmpty()
                            ) {
                                albumTracks
                            } else {
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
                    modifier = Modifier.menuReveal(N),
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
                    modifier = Modifier.menuReveal(N),
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
                modifier = Modifier.menuReveal(N),
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

    var playlists by remember {
        mutableStateOf(
            PlaylistStore.getPlaylists(
                context
            )
        )
    }

    var newPlaylistName by remember {
        mutableStateOf("")
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
                        text = "Create and Add"
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(
                        max = 260.dp
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