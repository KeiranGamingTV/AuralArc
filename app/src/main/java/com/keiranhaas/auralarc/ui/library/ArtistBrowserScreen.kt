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
import androidx.compose.foundation.shape.CircleShape

internal fun artistCategoryName(
    track: MusicTrack
): String {
    val albumArtist =
        track.albumArtist.trim()

    if (
        albumArtist.isNotBlank() &&
        !albumArtist.equals(
            "<unknown>",
            ignoreCase = true
        ) &&
        !albumArtist.equals(
            "Unknown Artist",
            ignoreCase = true
        )
    ) {
        return albumArtist
    }

    return track.artist
        .substringBefore(
            ";"
        )
        .trim()
        .ifBlank {
            "Unknown Artist"
        }
}

@Composable
fun ArtistBrowserScreen(
    tracks: List<MusicTrack>,
    onArtistSelected: (String) -> Unit,
    viewMode: LibraryViewMode = LibraryViewMode.LIST
) {
    val artists =
        tracks
            .groupBy { track ->
                artistCategoryName(
                    track
                ).lowercase()
            }
            .values
            .map { artistTracks ->
                artistCategoryName(
                    artistTracks.first()
                ) to artistTracks
            }
            .sortedBy { artistGroup ->
                artistGroup.first.lowercase()
            }
    if (
        viewMode == LibraryViewMode.GRID
    ) {
        ArtistGrid(
            artists = artists,
            onArtistSelected = onArtistSelected
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
            artists
        ) { artistGroup ->

            val artistName =
                artistGroup.first

            val artistTracks =
                artistGroup.second

            val albumCount =
                artistTracks
                    .map {
                        it.album
                    }
                    .distinct()
                    .size

            val artistArtPath =
                artistTracks
                    .firstOrNull {
                        !it.albumArtPath.isNullOrBlank()
                    }
                    ?.albumArtPath

            AuralArcCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    )
                    .auralArcClickable {
                        onArtistSelected(
                            artistName
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
                        albumArtPath = artistArtPath,
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
                            text = artistName,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AuralArcStyle.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                            if (
                                albumCount == 1
                            ) {
                                "1 album"
                            } else {
                                "$albumCount albums"
                            },
                            style = MaterialTheme.typography.body2,
                            color = AuralArcStyle.TextSecondary
                        )

                        Text(
                            text =
                            if (
                                artistTracks.size == 1
                            ) {
                                "1 song"
                            } else {
                                "${artistTracks.size} songs"
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
private fun ArtistGrid(
    artists: List<Pair<String, List<MusicTrack>>>,
    onArtistSelected: (String) -> Unit
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
            artists
        ) { artistGroup ->
            val artistName =
                artistGroup.first

            val artistTracks =
                artistGroup.second

            val albumCount =
                artistTracks
                    .map {
                        it.album
                    }
                    .distinct()
                    .size

            val artistArtPath =
                artistTracks
                    .firstOrNull {
                        !it.albumArtPath.isNullOrBlank()
                    }
                    ?.albumArtPath

            LibraryGridTile(
                title = artistName,
                subtitle =
                    if (
                        albumCount == 1
                    ) {
                        "1 album"
                    } else {
                        "$albumCount albums"
                    },
                onClick = {
                    onArtistSelected(
                        artistName
                    )
                }
            ) { artSize ->
                TrackArtwork(
                    albumArtPath = artistArtPath,
                    size = artSize,
                    shape = CircleShape
                )
            }
        }
    }
}