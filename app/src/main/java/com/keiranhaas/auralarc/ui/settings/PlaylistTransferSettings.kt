package com.keiranhaas.auralarc.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.Playlist
import com.keiranhaas.auralarc.scanner.LibraryManager
import com.keiranhaas.auralarc.storage.PlaylistStore
import com.keiranhaas.auralarc.storage.PlaylistTransferManager
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistTransferSettingsSection() {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var exportPlaylistPickerVisible by remember {
        mutableStateOf(
            false
        )
    }

    var availableExportPlaylists by remember {
        mutableStateOf<List<Playlist>>(
            emptyList()
        )
    }

    var pendingExportPlaylist by remember {
        mutableStateOf<Playlist?>(
            null
        )
    }

    var transferMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    suspend fun loadLocalTracksForTransfer() =
        withContext(
            Dispatchers.IO
        ) {
            val cachedTracks =
                LibraryRuntimeState.localTracks.value
                    ?: emptyList()

            if (
                cachedTracks.isNotEmpty()
            ) {
                cachedTracks
            } else {
                try {
                    LibraryManager.loadLibrary(
                        context.applicationContext
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

    val importM3uLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (
                uri == null
            ) {
                return@rememberLauncherForActivityResult
            }

            scope.launch {
                val tracks =
                    loadLocalTracksForTransfer()

                val result =
                    withContext(
                        Dispatchers.IO
                    ) {
                        PlaylistTransferManager.importM3u(
                            context = context.applicationContext,
                            uri = uri,
                            allTracks = tracks
                        )
                    }

                transferMessage =
                    if (
                        result.playlist != null
                    ) {
                        buildString {
                            append(
                                "Imported ${result.importedTrackCount} song"
                            )

                            if (
                                result.importedTrackCount != 1
                            ) {
                                append(
                                    "s"
                                )
                            }

                            append(
                                " into ‘${result.playlist.name}’."
                            )

                            if (
                                result.missingEntries.isNotEmpty()
                            ) {
                                append(
                                    "\n\n${result.missingEntries.size} entr"
                                )

                                append(
                                    if (
                                        result.missingEntries.size == 1
                                    ) {
                                        "y could not be matched to the local library."
                                    } else {
                                        "ies could not be matched to the local library."
                                    }
                                )
                            }
                        }
                    } else if (
                        tracks.isEmpty()
                    ) {
                        "AuralArc could not import the playlist because the local library is empty or unavailable."
                    } else {
                        "AuralArc could not match any entries in this M3U playlist to the local library."
                    }
            }
        }

    val exportM3uLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(
                "audio/x-mpegurl"
            )
        ) { uri ->
            val playlist =
                pendingExportPlaylist

            pendingExportPlaylist =
                null

            if (
                uri == null ||
                playlist == null
            ) {
                return@rememberLauncherForActivityResult
            }

            scope.launch {
                val tracks =
                    loadLocalTracksForTransfer()

                val exportedCount =
                    withContext(
                        Dispatchers.IO
                    ) {
                        PlaylistTransferManager.exportM3u(
                            context = context.applicationContext,
                            uri = uri,
                            playlist = playlist,
                            allTracks = tracks
                        )
                    }

                transferMessage =
                    "Exported $exportedCount song${if (exportedCount == 1) "" else "s"} from ‘${playlist.name}’."
            }
        }

    SettingsIconRow(
        title = "Import M3U Playlist",
        icon = Icons.Default.FileUpload,
        onClick = {
            importM3uLauncher.launch(
                arrayOf(
                    "audio/x-mpegurl",
                    "audio/mpegurl",
                    "application/x-mpegurl",
                    "application/vnd.apple.mpegurl",
                    "application/octet-stream",
                    "text/plain"
                )
            )
        }
    )

    SettingsIconRow(
        title = "Export M3U Playlist",
        icon = Icons.Default.FileDownload,
        onClick = {
            availableExportPlaylists =
                PlaylistStore.getPlaylists(
                    context
                ).filter { playlist ->
                    playlist.source == "LOCAL"
                }

            if (
                availableExportPlaylists.isEmpty()
            ) {
                transferMessage =
                    "Create a local playlist before exporting an M3U file."
            } else {
                exportPlaylistPickerVisible =
                    true
            }
        }
    )

    if (
        exportPlaylistPickerVisible
    ) {
        AlertDialog(
            onDismissRequest = {
                exportPlaylistPickerVisible =
                    false
            },
            title = {
                Text(
                    text = "Export M3U Playlist"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 360.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    Text(
                        text = "Choose the local playlist to export.",
                        color = AuralArcStyle.TextSecondary,
                        modifier = Modifier.padding(
                            bottom = 8.dp
                        )
                    )

                    availableExportPlaylists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .auralArcClickable {
                                    pendingExportPlaylist =
                                        playlist

                                    exportPlaylistPickerVisible =
                                        false

                                    val safeName =
                                        playlist.name
                                            .map { character ->
                                                if (
                                                    character in
                                                    setOf(
                                                        '\\',
                                                        '/',
                                                        ':',
                                                        '*',
                                                        '?',
                                                        '"',
                                                        '<',
                                                        '>',
                                                        '|'
                                                    )
                                                ) {
                                                    '_'
                                                } else {
                                                    character
                                                }
                                            }
                                            .joinToString(
                                                separator = ""
                                            )
                                            .ifBlank {
                                                "AuralArc Playlist"
                                            }

                                    exportM3uLauncher.launch(
                                        "$safeName.m3u"
                                    )
                                }
                                .padding(
                                    vertical = 12.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = AuralArcStyle.PurpleBright
                            )

                            Spacer(
                                modifier = Modifier.width(
                                    12.dp
                                )
                            )

                            Column {
                                Text(
                                    text = playlist.name,
                                    color = AuralArcStyle.TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "${playlist.trackCount} song${if (playlist.trackCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.caption,
                                    color = AuralArcStyle.TextMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportPlaylistPickerVisible =
                            false
                    }
                ) {
                    Text(
                        text = "Cancel"
                    )
                }
            },
            backgroundColor = AuralArcStyle.SurfaceBright,
            contentColor = AuralArcStyle.TextPrimary
        )
    }

    transferMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                transferMessage =
                    null
            },
            title = {
                Text(
                    text = "Playlist Transfer"
                )
            },
            text = {
                Text(
                    text = message,
                    color = AuralArcStyle.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        transferMessage =
                            null
                    }
                ) {
                    Text(
                        text = "OK"
                    )
                }
            },
            backgroundColor = AuralArcStyle.SurfaceBright,
            contentColor = AuralArcStyle.TextPrimary
        )
    }
}