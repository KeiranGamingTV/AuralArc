package com.example.auralarc.ui

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
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.ui.theme.AuralArcStyle

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
    onArtistSelected: (String) -> Unit
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

            Card(
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