package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.navigation.Screen
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.storage.PlaylistStore
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun QueueScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val listState =
        rememberLazyListState()

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

    val queueRevision =
        QueueManager.revision.value

    val queueEntries =
        remember(
            queueRevision
        ) {
            QueueManager.getQueueEntries()
        }

    val queue =
        remember(
            queueEntries
        ) {
            queueEntries.map { entry ->
                entry.track
            }
        }

    val currentIndex =
        QueueManager.currentIndex

    /*
     * Scroll to the currently-playing song once when the Queue
     * screen first opens.
     *
     * After that, the user's scroll position belongs to the user.
     * Queue edits must not force the list back to currentIndex.
     */
    LaunchedEffect(
        Unit
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
        containerColor =
            AuralArcStyle.BackgroundBottom,

        topBar = {
            com.keiranhaas.auralarc.ui.components.AuralArcTopBar(
                title =
                    "Queue",

                navigationIcon = {
                    com.keiranhaas.auralarc.ui.components.AuralArcBackButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    )
                },

                actions = {
                    AuralArcIconButton(
                        enabled =
                            queue.isNotEmpty(),

                        onClick = {
                            showSaveQueueDialog =
                                true
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.PlaylistAdd,

                            contentDescription =
                                "Save queue as playlist"
                        )
                    }

                    AuralArcIconButton(
                        enabled =
                            queue.isNotEmpty(),

                        onClick = {
                            showClearConfirm =
                                true
                        }
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.ClearAll,

                            contentDescription =
                                "Clear queue"
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
                        items = queueEntries,
                        key = { _, entry ->
                            entry.entryId
                        }
                    ) { index, entry ->

                        val track =
                            entry.track

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
                    text = "Clear Queue?",
                    color = AuralArcStyle.TextPrimary
                )
            },
            text = {
                Text(
                    text = "This removes every song from the current queue.",
                    color = AuralArcStyle.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        QueueManager.clearQueue()

                        PlayerManager.clearQueuePlayback(
                            context
                        )

                        showClearConfirm =
                            false
                    }
                ) {
                    Text(
                        text = "Clear",
                        color = AuralArcStyle.PurpleBright
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
                        text = "Cancel",
                        color = AuralArcStyle.TextSecondary
                    )
                }
            },
            containerColor = AuralArcStyle.Surface,
            titleContentColor = AuralArcStyle.TextPrimary,
            textContentColor = AuralArcStyle.TextSecondary
        )
    }
}

@Composable
private fun QueueHeaderCard(
    queue: List<MusicTrack>,
    currentIndex: Int
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
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text = "Current Queue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = "${queue.size} songs",
                style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.labelMedium,
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
                    style = MaterialTheme.typography.bodyLarge,
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
                    MaterialTheme.typography.bodyMedium,
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
                text = "Save Queue as Playlist",
                color = AuralArcStyle.TextPrimary
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
                        },
                    color = AuralArcStyle.TextSecondary
                )

                Spacer(
                    modifier = Modifier.height(
                        12.dp
                    )
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor =
                            AuralArcStyle.TextPrimary,
                        unfocusedTextColor =
                            AuralArcStyle.TextPrimary,
                        focusedContainerColor =
                            AuralArcStyle.SurfaceBright,
                        unfocusedContainerColor =
                            AuralArcStyle.SurfaceBright,
                        focusedLabelColor =
                            AuralArcStyle.PurpleBright,
                        unfocusedLabelColor =
                            AuralArcStyle.TextMuted,
                        focusedIndicatorColor =
                            AuralArcStyle.PurpleBright,
                        unfocusedIndicatorColor =
                            AuralArcStyle.TextMuted,
                        cursorColor =
                            AuralArcStyle.PurpleBright
                    )
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
                    text = "Save",
                    color =
                        if (
                            playlistName.isNotBlank()
                        ) {
                            AuralArcStyle.PurpleBright
                        } else {
                            AuralArcStyle.TextMuted
                        }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = AuralArcStyle.TextSecondary
                )
            }
        },
        containerColor = AuralArcStyle.Surface,
        titleContentColor = AuralArcStyle.TextPrimary,
        textContentColor = AuralArcStyle.TextSecondary
    )
}