package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.storage.AppearancePreferences
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun MusicLibraryScreen(
    tracks: List<MusicTrack>,
    showAddAlbumToPlaylist: Boolean = false,
    onOpenLyrics: (MusicTrack) -> Unit = {},
    onOpenTrackInfo: (MusicTrack) -> Unit = {}
) {
    val context =
        LocalContext.current

    if (
        tracks.isEmpty()
    ) {
        AuralArcMessageCard(
            title = "No Songs Found",
            message = "No songs matched the current library, source, or search."
        )

        return
    }

    val compactRows =
        remember {
            AppearancePreferences.getCompactRows(
                context
            )
        }

    val showArtwork =
        remember {
            AppearancePreferences.getShowArtwork(
                context
            )
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 10.dp
        )
    ) {
        items(
            items = tracks,
            key = { track ->
                track.uri
            }
        ) { track ->
            MusicTrackRow(
                track = track,
                queueTracks = tracks,
                showAddAlbumToPlaylist = showAddAlbumToPlaylist,
                compactRows = compactRows,
                showArtwork = showArtwork,
                onClick = {
                    QueueManager.setQueue(
                        tracks,
                        track
                    )

                    PlayerManager.playTrack(
                        context = context,
                        track = track,
                        queueTracks = QueueManager.getQueue()
                    )
                },
                onOpenLyrics = {
                    onOpenLyrics(
                        track
                    )
                },
                onOpenTrackInfo = {
                    onOpenTrackInfo(
                        track
                    )
                }
            )
        }
    }
}

@Composable
private fun MusicTrackRow(
    track: MusicTrack,
    queueTracks: List<MusicTrack>,
    showAddAlbumToPlaylist: Boolean,
    compactRows: Boolean,
    showArtwork: Boolean,
    onClick: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenTrackInfo: () -> Unit
) {
    val rowPadding =
        if (
            compactRows
        ) {
            7.dp
        } else {
            10.dp
        }

    val artworkSize =
        if (
            compactRows
        ) {
            44.dp
        } else {
            54.dp
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical =
                if (
                    compactRows
                ) {
                    3.dp
                } else {
                    5.dp
                }
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
                    rowPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (
                showArtwork
            ) {
                TrackArtwork(
                    albumArtPath = track.albumArtPath,
                    size = artworkSize
                )

                Spacer(
                    modifier = Modifier.width(
                        12.dp
                    )
                )
            }

            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                TrackTitleWithHdBadge(
                    track = track,
                    style =
                    MaterialTheme.typography.body1,
                    color =
                    AuralArcStyle.TextPrimary,
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

            MoreOptionsButton(
                track = track,
                queueTracks = queueTracks,
                showAddAlbumToPlaylist = showAddAlbumToPlaylist,
                onOpenLyrics = {
                    onOpenLyrics()
                },
                onOpenTrackInfo = {
                    onOpenTrackInfo()
                }
            )
        }
    }
}