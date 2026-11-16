package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
fun AlbumBrowserScreen(
    tracks: List<MusicTrack>,
    onAlbumSelected: (String) -> Unit,
    viewMode: LibraryViewMode = LibraryViewMode.LIST
) {
    val albums =
        tracks
            .groupBy {
                it.album
            }
            .toList()
            .sortedBy {
                it.first.lowercase()
            }
    if (
        viewMode == LibraryViewMode.GRID
    ) {
        AlbumGrid(
            albums = albums,
            onAlbumSelected = onAlbumSelected
        )

        return
    }

    LazyColumn(
        contentPadding =
        PaddingValues(
            top = 6.dp,
            bottom = 12.dp
        )
    ) {
        items(
            albums
        ) { albumGroup ->

            val albumName =
                albumGroup.first

            val albumTracks =
                albumGroup.second

            val albumArtPath =
                albumTracks
                    .firstOrNull {
                        !it.albumArtPath.isNullOrBlank()
                    }
                    ?.albumArtPath

            val artistText =
                albumTracks
                    .map {
                        it.artist
                    }
                    .distinct()
                    .take(
                        2
                    )
                    .joinToString(
                        ", "
                    )
                    .ifBlank {
                        "Unknown Artist"
                    }

            AuralArcCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    )
                    .auralArcClickable {
                        onAlbumSelected(
                            albumName
                        )
                    },
                shape = AuralArcStyle.CardShape,
                backgroundColor = AuralArcStyle.Surface,
                elevation = 7.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(
                        albumArtPath = albumArtPath,
                        size = 72.dp
                    )

                    Spacer(
                        modifier =
                        Modifier.width(
                            14.dp
                        )
                    )

                    Column(
                        modifier =
                        Modifier.weight(
                            1f
                        )
                    ) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AuralArcStyle.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = artistText,
                            style = MaterialTheme.typography.body2,
                            color = AuralArcStyle.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                            if (
                                albumTracks.size == 1
                            ) {
                                "1 song"
                            } else {
                                "${albumTracks.size} songs"
                            },
                            style = MaterialTheme.typography.caption,
                            color = AuralArcStyle.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Pair<String, List<MusicTrack>>>,
    onAlbumSelected: (String) -> Unit
) {
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
        gridItems(
            albums
        ) { albumGroup ->
            val albumName =
                albumGroup.first

            val albumTracks =
                albumGroup.second

            val albumArtPath =
                albumTracks
                    .firstOrNull {
                        !it.albumArtPath.isNullOrBlank()
                    }
                    ?.albumArtPath

            val artistText =
                albumTracks
                    .map {
                        it.artist
                    }
                    .distinct()
                    .take(
                        2
                    )
                    .joinToString(
                        ", "
                    )
                    .ifBlank {
                        "Unknown Artist"
                    }

            LibraryGridTile(
                title = albumName,
                subtitle = artistText,
                onClick = {
                    onAlbumSelected(
                        albumName
                    )
                }
            ) { artSize ->
                TrackArtwork(
                    albumArtPath = albumArtPath,
                    size = artSize
                )
            }
        }
    }
}