package com.keiranhaas.auralarc.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
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
import com.keiranhaas.auralarc.player.PlaybackState
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.storage.PlaylistStore
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import com.keiranhaas.auralarc.storage.PlaylistArtworkStore
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    allTracks: List<MusicTrack>,
    librarySource: LibrarySource,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val coroutineScope =
        rememberCoroutineScope()

    var playlist by remember(
        playlistId,
        librarySource
    ) {
        mutableStateOf<Playlist?>(
            null
        )
    }

    var playlistTracks by remember(
        playlistId,
        librarySource
    ) {
        mutableStateOf<List<MusicTrack>>(
            emptyList()
        )
    }

    var isLoading by remember(
        playlistId,
        librarySource
    ) {
        mutableStateOf(
            true
        )
    }

    var showRenameDialog by remember {
        mutableStateOf(
            false
        )
    }

    var showDeleteDialog by remember {
        mutableStateOf(
            false
        )
    }

    var artworkRefreshKey by remember(
        playlistId,
        librarySource
    ) {
        mutableStateOf(
            0
        )
    }

    val playlistArtworkPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { selectedUri ->
            val targetPlaylist =
                playlist

            if (
                selectedUri != null &&
                targetPlaylist != null
            ) {
                coroutineScope.launch {
                    val savedArtworkPath =
                        withContext(
                            Dispatchers.IO
                        ) {
                            PlaylistArtworkStore.setCustomArtwork(
                                context = context,
                                playlist = targetPlaylist,
                                sourceUri = selectedUri
                            )
                        }

                    if (
                        !savedArtworkPath.isNullOrBlank()
                    ) {
                        artworkRefreshKey +=
                            1
                    }
                }
            }
        }

    suspend fun reloadPlaylistData() {
        isLoading =
            true

        when (
            librarySource
        ) {
            LibrarySource.LOCAL -> {
                val loadedPlaylist =
                    PlaylistStore.getPlaylist(
                        context = context,
                        playlistId = playlistId
                    )

                playlist =
                    loadedPlaylist

                playlistTracks =
                    if (
                        loadedPlaylist != null
                    ) {
                        PlaylistStore.getTracksForPlaylist(
                            playlist = loadedPlaylist,
                            allTracks = allTracks
                        )
                    } else {
                        emptyList()
                    }
            }

            LibrarySource.NAVIDROME -> {
                val remoteDetail =
                    withContext(
                        Dispatchers.IO
                    ) {
                        NavidromePlaylistManager.loadPlaylistDetail(
                            context = context,
                            playlistId = playlistId
                        )
                    }

                playlist =
                    remoteDetail?.first

                playlistTracks =
                    remoteDetail?.second ?: emptyList()
            }
        }

        isLoading =
            false
    }

    LaunchedEffect(
        playlistId,
        librarySource
    ) {
        reloadPlaylistData()
    }

    if (
        isLoading
    ) {
        LoadingPlaylistScreen()

        return
    }

    val currentPlaylist =
        playlist ?: run {
            MissingPlaylistScreen(
                onBack = onBack
            )

            return
        }

    val canEdit =
        librarySource == LibrarySource.LOCAL

    val playlistListState =
        rememberLazyListState()

    val headerCollapseThresholdPx =
        with(
            androidx.compose.ui.platform.LocalDensity.current
        ) {
            56.dp.roundToPx()
        }

    val headerCollapsed by remember(
        playlistListState,
        headerCollapseThresholdPx
    ) {
        derivedStateOf {
            playlistListState.firstVisibleItemIndex > 0 ||
                    playlistListState.firstVisibleItemScrollOffset >
                    headerCollapseThresholdPx
        }
    }

    val hasCustomArtwork =
        remember(
            currentPlaylist.id,
            currentPlaylist.source,
            artworkRefreshKey
        ) {
            PlaylistArtworkStore.getCustomArtworkPath(
                context = context,
                playlist = currentPlaylist
            ) != null
        }

    val movePlaylistTrack: (
        Int,
        Int
    ) -> Unit = { fromIndex, toIndex ->
        if (
            canEdit &&
            fromIndex in playlistTracks.indices &&
            toIndex in playlistTracks.indices
        ) {
            PlaylistStore.moveTrackInPlaylist(
                context = context,
                playlistId = playlistId,
                fromIndex = fromIndex,
                toIndex = toIndex
            )

            val reorderedTracks =
                playlistTracks.toMutableList()

            val movedTrack =
                reorderedTracks.removeAt(
                    fromIndex
                )

            reorderedTracks.add(
                toIndex,
                movedTrack
            )

            playlistTracks =
                reorderedTracks

            playlist =
                PlaylistStore.getPlaylist(
                    context = context,
                    playlistId = playlistId
                )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PlaylistDetailHeader(
            playlist = currentPlaylist,
            tracks = playlistTracks,
            trackCount = playlistTracks.size,
            totalDuration = playlistDurationText(
                playlistTracks
            ),
            canEdit = canEdit,
            collapsed = headerCollapsed,
            artworkRefreshKey = artworkRefreshKey,
            hasCustomArtwork = hasCustomArtwork,
            onBack = onBack,
            onRename = {
                showRenameDialog =
                    true
            },
            onDelete = {
                showDeleteDialog =
                    true
            },
            onChangeArtwork = {
                playlistArtworkPicker.launch(
                    "image/*"
                )
            },
            onRemoveArtwork = {
                PlaylistArtworkStore.removeCustomArtwork(
                    context = context,
                    playlist = currentPlaylist
                )

                artworkRefreshKey +=
                    1
            },
            onPlay = {
                playPlaylistTracks(
                    context = context,
                    tracks = playlistTracks,
                    shuffle = false
                )
            },
            onShuffle = {
                playPlaylistTracks(
                    context = context,
                    tracks = playlistTracks,
                    shuffle = true
                )
            }
        )

        if (
            playlistTracks.isEmpty()
        ) {
            EmptyPlaylistDetail(
                librarySource = librarySource
            )
        } else {
            LazyColumn(
                state = playlistListState,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                itemsIndexed(
                    items = playlistTracks
                ) { index, track ->
                    PlaylistTrackRow(
                        track = track,
                        canRemove = canEdit,
                        canMoveUp =
                            canEdit &&
                                    index > 0,
                        canMoveDown =
                            canEdit &&
                                    index < playlistTracks.lastIndex,
                        onClick = {
                            QueueManager.setQueue(
                                playlistTracks,
                                track
                            )

                            PlayerManager.playTrack(
                                context = context,
                                track = track,
                                queueTracks = playlistTracks
                            )
                        },
                        onMoveUp = {
                            movePlaylistTrack(
                                index,
                                index - 1
                            )
                        },
                        onMoveDown = {
                            movePlaylistTrack(
                                index,
                                index + 1
                            )
                        },
                        onRemove = {
                            if (
                                canEdit
                            ) {
                                PlaylistStore.removeTrackFromPlaylist(
                                    context = context,
                                    playlistId = playlistId,
                                    trackUri = track.uri
                                )

                                val loadedPlaylist =
                                    PlaylistStore.getPlaylist(
                                        context = context,
                                        playlistId = playlistId
                                    )

                                playlist =
                                    loadedPlaylist

                                playlistTracks =
                                    if (
                                        loadedPlaylist != null
                                    ) {
                                        PlaylistStore.getTracksForPlaylist(
                                            playlist = loadedPlaylist,
                                            allTracks = allTracks
                                        )
                                    } else {
                                        emptyList()
                                    }
                            }
                        }
                    )
                }
            }
        }
    }

    if (
        showRenameDialog &&
        canEdit
    ) {
        RenamePlaylistDialog(
            currentName = currentPlaylist.name,
            onDismiss = {
                showRenameDialog =
                    false
            },
            onRename = { newName ->
                PlaylistStore.renamePlaylist(
                    context = context,
                    playlistId = playlistId,
                    newName = newName
                )

                playlist =
                    PlaylistStore.getPlaylist(
                        context = context,
                        playlistId = playlistId
                    )

                showRenameDialog =
                    false
            }
        )
    }

    if (
        showDeleteDialog &&
        canEdit
    ) {
        DeletePlaylistConfirmationDialog(
            playlistName = currentPlaylist.name,
            onDismiss = {
                showDeleteDialog =
                    false
            },
            onDelete = {
                PlaylistArtworkStore.removeCustomArtwork(
                    context = context,
                    playlist = currentPlaylist
                )

                PlaylistStore.deletePlaylist(
                    context = context,
                    playlistId = playlistId
                )

                showDeleteDialog =
                    false

                onBack()
            }
        )
    }
}

@Composable
private fun PlaylistDetailHeader(
    playlist: Playlist,
    tracks: List<MusicTrack>,
    trackCount: Int,
    totalDuration: String,
    canEdit: Boolean,
    collapsed: Boolean,
    artworkRefreshKey: Int,
    hasCustomArtwork: Boolean,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChangeArtwork: () -> Unit,
    onRemoveArtwork: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val sourceLabel =
        if (
            playlist.source == "NAVIDROME"
        ) {
            "Navidrome"
        } else {
            "Local"
        }

    val songCountText =
        if (
            trackCount == 1
        ) {
            "1 song • $totalDuration"
        } else {
            "$trackCount songs • $totalDuration"
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical =
                    if (
                        collapsed
                    ) {
                        4.dp
                    } else {
                        8.dp
                    }
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = {
                (
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 220,
                                delayMillis = 60,
                                easing = FastOutSlowInEasing
                            )
                        )
                            .togetherWith(
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = 160,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            )
                        )
                    .using(
                        SizeTransform(
                            clip = false,
                            sizeAnimationSpec = { _, _ ->
                                tween(
                                    durationMillis = 320,
                                    easing = FastOutSlowInEasing
                                )
                            }
                        )
                    )
            },
            label = "PlaylistHeaderCollapse"
        ) { isCollapsed ->

            if (
                isCollapsed
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            7.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuralArcIconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to playlists",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }

                    PlaylistArtwork(
                        playlist = playlist,
                        tracks = tracks,
                        size = 44.dp,
                        refreshKey = artworkRefreshKey
                    )

                    Spacer(
                        modifier = Modifier.width(
                            10.dp
                        )
                    )

                    Column(
                        modifier = Modifier.weight(
                            1f
                        )
                    ) {
                        Text(
                            text = playlist.name,
                            style =
                                MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AuralArcStyle.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = songCountText,
                            style = MaterialTheme.typography.caption,
                            color = AuralArcStyle.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AuralArcIconButton(
                        enabled = trackCount > 0,
                        onClick = onPlay
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play playlist",
                            tint = AuralArcStyle.PurpleBright
                        )
                    }

                    AuralArcIconButton(
                        enabled = trackCount > 0,
                        onClick = onShuffle
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle playlist",
                            tint = AuralArcStyle.PurpleBright
                        )
                    }

                    PlaylistHeaderOptionsButton(
                        canEdit = canEdit,
                        hasCustomArtwork = hasCustomArtwork,
                        onRename = onRename,
                        onDelete = onDelete,
                        onChangeArtwork = onChangeArtwork,
                        onRemoveArtwork = onRemoveArtwork
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(
                        12.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuralArcIconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to playlists",
                                tint = AuralArcStyle.TextPrimary
                            )
                        }

                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.caption,
                            color = AuralArcStyle.TextMuted
                        )

                        Spacer(
                            modifier = Modifier.weight(
                                1f
                            )
                        )

                        PlaylistHeaderOptionsButton(
                            canEdit = canEdit,
                            hasCustomArtwork = hasCustomArtwork,
                            onRename = onRename,
                            onDelete = onDelete,
                            onChangeArtwork = onChangeArtwork,
                            onRemoveArtwork = onRemoveArtwork
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistArtwork(
                            playlist = playlist,
                            tracks = tracks,
                            size = 86.dp,
                            refreshKey = artworkRefreshKey
                        )

                        Spacer(
                            modifier = Modifier.width(
                                14.dp
                            )
                        )

                        Column(
                            modifier = Modifier.weight(
                                1f
                            )
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = AuralArcStyle.TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = songCountText,
                                style = MaterialTheme.typography.body2,
                                color = AuralArcStyle.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuralArcIconButton(
                                enabled = trackCount > 0,
                                onClick = onPlay
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play playlist",
                                    tint = AuralArcStyle.PurpleBright
                                )
                            }

                            AuralArcIconButton(
                                enabled = trackCount > 0,
                                onClick = onShuffle
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle playlist",
                                    tint = AuralArcStyle.PurpleBright
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeaderOptionsButton(
    canEdit: Boolean,
    hasCustomArtwork: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChangeArtwork: () -> Unit,
    onRemoveArtwork: () -> Unit
) {
    var menuExpanded by remember {
        mutableStateOf(
            false
        )
    }

    Box {
        AuralArcIconButton(
            onClick = {
                menuExpanded =
                    true
            }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Playlist options",
                tint = AuralArcStyle.TextPrimary
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded =
                    false
            }
        ) {
            DropdownMenuItem(
                onClick = {
                    menuExpanded =
                        false

                    onChangeArtwork()
                }
            ) {
                Text(
                    text =
                        if (
                            hasCustomArtwork
                        ) {
                            "Change Playlist Artwork"
                        } else {
                            "Choose Playlist Artwork"
                        }
                )
            }

            if (
                hasCustomArtwork
            ) {
                DropdownMenuItem(
                    onClick = {
                        menuExpanded =
                            false

                        onRemoveArtwork()
                    }
                ) {
                    Text(
                        text = "Remove Custom Artwork"
                    )
                }
            }

            if (
                canEdit
            ) {
                DropdownMenuItem(
                    onClick = {
                        menuExpanded =
                            false

                        onRename()
                    }
                ) {
                    Text(
                        text = "Rename Playlist"
                    )
                }

                DropdownMenuItem(
                    onClick = {
                        menuExpanded =
                            false

                        onDelete()
                    }
                ) {
                    Text(
                        text = "Delete Playlist"
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: MusicTrack,
    canRemove: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember(
        track.uri
    ) {
        mutableStateOf(
            false
        )
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
                    10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(
                albumArtPath = track.albumArtPath,
                size = 54.dp
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
                TrackTitleWithHdBadge(
                    track = track,
                    style = MaterialTheme.typography.body1,
                    color = AuralArcStyle.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fillTitleWeight = true
                )

                Text(
                    text =
                        trackArtistAlbumText(
                            track
                        ),
                    style =
                        MaterialTheme.typography.body2,
                    color =
                        AuralArcStyle.TextSecondary,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            if (
                canRemove
            ) {
                Column {
                    AuralArcIconButton(
                        enabled = canMoveUp,
                        onClick = onMoveUp
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move song up",
                            tint =
                                if (
                                    canMoveUp
                                ) {
                                    AuralArcStyle.TextPrimary
                                } else {
                                    AuralArcStyle.TextMuted
                                }
                        )
                    }

                    AuralArcIconButton(
                        enabled = canMoveDown,
                        onClick = onMoveDown
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move song down",
                            tint =
                                if (
                                    canMoveDown
                                ) {
                                    AuralArcStyle.TextPrimary
                                } else {
                                    AuralArcStyle.TextMuted
                                }
                        )
                    }
                }

                Box {
                    AuralArcIconButton(
                        onClick = {
                            menuExpanded =
                                true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Song options",
                            tint = AuralArcStyle.TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded =
                                false
                        }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                menuExpanded =
                                    false

                                onRemove()
                            }
                        ) {
                            Text(
                                text = "Remove from Playlist"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistDetail(
    librarySource: LibrarySource
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
            text = "This playlist is empty",
            style = MaterialTheme.typography.h6,
            color = AuralArcStyle.TextPrimary
        )

        Text(
            text =
                if (
                    librarySource == LibrarySource.NAVIDROME
                ) {
                    "Add songs to this playlist in Navidrome, then refresh."
                } else {
                    "Go to your library and add songs to this playlist."
                },
            style = MaterialTheme.typography.body2,
            color = AuralArcStyle.TextMuted
        )
    }
}

@Composable
private fun LoadingPlaylistScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Loading playlist...",
            style = MaterialTheme.typography.h6,
            color = AuralArcStyle.TextPrimary
        )

        Text(
            text = "Please wait.",
            style = MaterialTheme.typography.body2,
            color = AuralArcStyle.TextMuted
        )
    }
}

@Composable
private fun MissingPlaylistScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Playlist not found",
            style = MaterialTheme.typography.h6,
            color = AuralArcStyle.TextPrimary
        )

        AuralArcButton(
            onClick = onBack,
            modifier = Modifier.padding(
                top = 12.dp
            )
        ) {
            Text(
                text = "Back"
            )
        }
    }
}

@Composable
private fun DeletePlaylistConfirmationDialog(
    playlistName: String,
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
                text =
                    "Delete \"$playlistName\"? This will remove the playlist, but it will not delete any audio files."
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
private fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var playlistName by remember {
        mutableStateOf(
            currentName
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Playlist"
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
                    onRename(
                        playlistName
                    )
                }
            ) {
                Text(
                    text = "Rename"
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

private fun playPlaylistTracks(
    context: Context,
    tracks: List<MusicTrack>,
    shuffle: Boolean
) {
    if (
        tracks.isEmpty()
    ) {
        return
    }

    val startTrack =
        if (
            shuffle
        ) {
            tracks.random()
        } else {
            tracks.first()
        }

    PlaybackState.shuffleEnabled.value =
        shuffle

    if (
        shuffle
    ) {
        QueueManager.setQueue(
            tracks,
            startTrack
        )
    } else {
        QueueManager.setQueuePreservingOrder(
            tracks,
            startTrack
        )
    }

    PlayerManager.playTrack(
        context = context,
        track = startTrack,
        queueTracks = tracks
    )
}

private fun playlistDurationText(
    tracks: List<MusicTrack>
): String {
    val totalMillis =
        tracks.map {
            it.duration
        }.sum()

    val totalSeconds =
        totalMillis / 1000L

    val hours =
        totalSeconds / 3600L

    val minutes =
        (totalSeconds % 3600L) / 60L

    return if (
        hours > 0L
    ) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}