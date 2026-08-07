package com.example.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.navigation.Screen
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.player.QueueManager
import com.example.auralarc.storage.PlaylistStore
import com.example.auralarc.ui.theme.AuralArcStyle

@Composable
fun QueueScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val listState =
        rememberLazyListState()

    var refreshKey by remember {
        mutableStateOf(
            0
        )
    }

    var showSaveQueueDialog by remember {
        mutableStateOf(
            false
        )
    }

    var showClearConfirm by remember {
        mutableStateOf(
            false
        )
    }

    val queue =
        remember(
            refreshKey,
            PlayerManager.currentTitle.value,
            PlayerManager.currentPosition.value
        ) {
            QueueManager.getQueue()
        }

    val currentIndex =
        QueueManager.currentIndex

    LaunchedEffect(
        currentIndex,
        queue.size
    ) {
        if (
            currentIndex in queue.indices
        ) {
            listState.scrollToItem(
                currentIndex
            )
        }
    }

    Scaffold(
        backgroundColor = AuralArcStyle.BackgroundBottom,
        topBar = {
            TopAppBar(
                backgroundColor = AuralArcStyle.BackgroundTop,
                elevation = 0.dp,
                navigationIcon = {
                    AuralArcIconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = "Queue",
                        color = AuralArcStyle.TextPrimary
                    )
                },
                actions = {
                    AuralArcIconButton(
                        enabled = queue.isNotEmpty(),
                        onClick = {
                            showSaveQueueDialog =
                                true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Save queue as playlist",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }

                    AuralArcIconButton(
                        enabled = queue.isNotEmpty(),
                        onClick = {
                            showClearConfirm =
                                true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear queue",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .background(
                    brush = AuralArcStyle.appBackgroundBrush()
                )
        ) {
            if (
                queue.isEmpty()
            ) {
                AuralArcMessageCard(
                    title = "Queue is Empty",
                    message = "Play a song or use More Options > Add to Queue."
                )
            } else {
                QueueHeaderCard(
                    queue = queue,
                    currentIndex = currentIndex
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 10.dp
                    )
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { _, track ->
                            track.uri
                        }
                    ) { index, track ->

                        QueueTrackRow(
                            track = track,
                            index = index,
                            isCurrent = index == currentIndex,
                            canMoveUp = index > 0,
                            canMoveDown = index < queue.lastIndex,
                            queueTracks = queue,

                            onClick = {
                                val selectedTrack =
                                    QueueManager.playFromQueue(
                                        index
                                    )

                                if (
                                    selectedTrack != null
                                ) {
                                    PlayerManager.playTrack(
                                        context = context,
                                        track = selectedTrack,
                                        queueTracks = QueueManager.getQueue()
                                    )
                                }

                                refreshKey++
                            },

                            onMoveUp = {
                                QueueManager.moveUp(
                                    index
                                )

                                /*
                                 * Important:
                                 *
                                 * The visible QueueManager order has changed.
                                 * Now rebuild Media3's actual playlist so playback
                                 * follows the same order.
                                 */
                                PlayerManager.syncPlayerPlaylistToQueueOrder(
                                    context = context
                                )

                                refreshKey++
                            },

                            onMoveDown = {
                                QueueManager.moveDown(
                                    index
                                )

                                /*
                                 * Keep the real Media3 playlist synchronized
                                 * with the newly reordered queue.
                                 */
                                PlayerManager.syncPlayerPlaylistToQueueOrder(
                                    context = context
                                )

                                refreshKey++
                            },

                            onRemove = {
                                val removedCurrent =
                                    QueueManager.removeAt(
                                        index
                                    )

                                PlayerManager.syncQueueAfterQueueChange(
                                    context = context,
                                    resetPosition = removedCurrent
                                )

                                refreshKey++
                            },

                            onOpenLyrics = {
                                NowPlayingLyricsRequest.request(
                                    track
                                )

                                navController.navigate(
                                    Screen.NowPlaying.route
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (
        showSaveQueueDialog
    ) {
        SaveQueueAsPlaylistDialog(
            queue = queue,
            onDismiss = {
                showSaveQueueDialog =
                    false
            }
        )
    }

    if (
        showClearConfirm
    ) {
        AlertDialog(
            onDismissRequest = {
                showClearConfirm =
                    false
            },
            title = {
                Text(
                    text = "Clear Queue?"
                )
            },
            text = {
                Text(
                    text = "This removes every song from the current queue."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        QueueManager.clearQueue()

                        PlayerManager.clearQueuePlayback(
                            context
                        )

                        refreshKey++

                        showClearConfirm =
                            false
                    }
                ) {
                    Text(
                        text = "Clear"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearConfirm =
                            false
                    }
                ) {
                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }
}

@Composable
private fun QueueHeaderCard(
    queue: List<MusicTrack>,
    currentIndex: Int
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
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text = "Current Queue",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = "${queue.size} songs",
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted
            )
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: MusicTrack,
    index: Int,
    isCurrent: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    queueTracks: List<MusicTrack>,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenLyrics: () -> Unit
) {
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
        backgroundColor =
        if (
            isCurrent
        ) {
            AuralArcStyle.SurfaceBright
        } else {
            AuralArcStyle.Surface
        },
        elevation =
        if (
            isCurrent
        ) {
            8.dp
        } else {
            5.dp
        }
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
                modifier = Modifier.weight(
                    1f
                )
            ) {
                Text(
                    text =
                    if (
                        isCurrent
                    ) {
                        "Now Playing"
                    } else {
                        "Queue #${index + 1}"
                    },
                    style = MaterialTheme.typography.caption,
                    color =
                    if (
                        isCurrent
                    ) {
                        AuralArcStyle.PurpleBright
                    } else {
                        AuralArcStyle.TextMuted
                    }
                )

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

            Column {
                AuralArcIconButton(
                    enabled = canMoveUp,
                    onClick = onMoveUp
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = AuralArcStyle.TextPrimary
                    )
                }

                AuralArcIconButton(
                    enabled = canMoveDown,
                    onClick = onMoveDown
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = AuralArcStyle.TextPrimary
                    )
                }
            }

            AuralArcIconButton(
                onClick = onRemove
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from queue",
                    tint = AuralArcStyle.TextMuted
                )
            }

            MoreOptionsButton(
                track = track,
                queueTracks = queueTracks,
                onOpenLyrics = {
                    onOpenLyrics()
                }
            )
        }
    }
}

@Composable
private fun SaveQueueAsPlaylistDialog(
    queue: List<MusicTrack>,
    onDismiss: () -> Unit
) {
    val context =
        LocalContext.current

    var playlistName by remember {
        mutableStateOf(
            ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Queue as Playlist"
            )
        },
        text = {
            Column {
                Text(
                    text =
                    if (
                        queue.size == 1
                    ) {
                        "1 song will be saved."
                    } else {
                        "${queue.size} songs will be saved."
                    }
                )

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
            }
        },
        confirmButton = {
            TextButton(
                enabled = playlistName.isNotBlank(),
                onClick = {
                    PlaylistStore.createPlaylistFromTracks(
                        context = context,
                        rawName = playlistName,
                        tracks = queue
                    )

                    onDismiss()
                }
            ) {
                Text(
                    text = "Save"
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