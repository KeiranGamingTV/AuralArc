package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.storage.GenreListeningStats
import com.keiranhaas.auralarc.storage.GroupListeningStats
import com.keiranhaas.auralarc.storage.ListeningStatsRange
import com.keiranhaas.auralarc.storage.ListeningStatsStore
import com.keiranhaas.auralarc.storage.ListeningStatsSummary
import com.keiranhaas.auralarc.storage.TrackListeningStats
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ListeningStatsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val localTracks =
        LibraryRuntimeState
            .localTracks
            .value
            .orEmpty()

    val navidromeTracks =
        LibraryRuntimeState
            .navidromeTracks
            .value
            .orEmpty()

    val allTracks =
        remember(
            localTracks,
            navidromeTracks
        ) {
            (
                    localTracks +
                            navidromeTracks
                    )
                .distinctBy { track ->
                    track.uri
                }
        }

    val metadataFingerprint =
        remember(
            allTracks
        ) {
            allTracks.fold(
                17
            ) { result, track ->
                31 * result +
                        track.uri.hashCode() +
                        track.genre.hashCode()
            }
        }

    var selectedRange by rememberSaveable {
        mutableStateOf(
            ListeningStatsRange.ALL_TIME
        )
    }

    var metadataRevision by remember {
        mutableStateOf(
            0
        )
    }

    var summary by remember {
        mutableStateOf<ListeningStatsSummary?>(
            null
        )
    }

    LaunchedEffect(
        metadataFingerprint
    ) {
        withContext(
            Dispatchers.IO
        ) {
            ListeningStatsStore.refreshTrackMetadata(
                context =
                context.applicationContext,
                tracks =
                allTracks
            )
        }

        metadataRevision++
    }

    LaunchedEffect(
        selectedRange,
        metadataRevision
    ) {
        summary =
            withContext(
                Dispatchers.IO
            ) {
                ListeningStatsStore.getSummary(
                    context =
                    context.applicationContext,
                    range =
                    selectedRange
                )
            }
    }

    Scaffold(
        backgroundColor =
        AuralArcStyle.BackgroundBottom,
        topBar = {
            TopAppBar(
                backgroundColor =
                AuralArcStyle.BackgroundTop,
                elevation = 0.dp,
                navigationIcon = {
                    AuralArcIconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                            "Back",
                            tint =
                            AuralArcStyle.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text =
                        "Listening Stats",
                        color =
                        AuralArcStyle.TextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        val currentSummary =
            summary

        if (
            currentSummary == null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .background(
                        brush =
                        AuralArcStyle
                            .appBackgroundBrush()
                    ),
                contentAlignment =
                Alignment.Center
            ) {
                CircularProgressIndicator(
                    color =
                    AuralArcStyle.PurpleBright
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .background(
                        brush =
                        AuralArcStyle
                            .appBackgroundBrush()
                    ),
                contentPadding =
                PaddingValues(
                    bottom = 18.dp
                )
            ) {
                item {
                    StatsRangeSelector(
                        selectedRange =
                        selectedRange,
                        onRangeSelected = { range ->
                            if (
                                range != selectedRange
                            ) {
                                summary =
                                    null

                                selectedRange =
                                    range
                            }
                        }
                    )
                }

                item {
                    StatsSummaryCard(
                        summary =
                        currentSummary
                    )
                }

                item {
                    SectionTitle(
                        title =
                        "Top Songs"
                    )
                }

                if (
                    currentSummary
                        .topTracks
                        .isEmpty()
                ) {
                    item {
                        EmptyStatsMessage(
                            text =
                            emptyRangeMessage(
                                selectedRange
                            )
                        )
                    }
                } else {
                    currentSummary
                        .topTracks
                        .forEach { stat ->
                            item(
                                key =
                                "track_${stat.trackKey}"
                            ) {
                                TrackStatRow(
                                    stat = stat
                                )
                            }
                        }
                }

                item {
                    SectionTitle(
                        title =
                        "Top Artists"
                    )
                }

                if (
                    currentSummary
                        .topArtists
                        .isEmpty()
                ) {
                    item {
                        EmptyStatsMessage(
                            text =
                            emptyRangeMessage(
                                selectedRange
                            )
                        )
                    }
                } else {
                    currentSummary
                        .topArtists
                        .forEach { stat ->
                            item(
                                key =
                                "artist_${stat.name}"
                            ) {
                                GroupStatRow(
                                    stat = stat
                                )
                            }
                        }
                }

                item {
                    SectionTitle(
                        title =
                        "Top Albums"
                    )
                }

                if (
                    currentSummary
                        .topAlbums
                        .isEmpty()
                ) {
                    item {
                        EmptyStatsMessage(
                            text =
                            emptyRangeMessage(
                                selectedRange
                            )
                        )
                    }
                } else {
                    currentSummary
                        .topAlbums
                        .forEach { stat ->
                            item(
                                key =
                                "album_${stat.name}"
                            ) {
                                GroupStatRow(
                                    stat = stat
                                )
                            }
                        }
                }

                item {
                    SectionTitle(
                        title =
                        "Genre Breakdown"
                    )
                }

                if (
                    currentSummary
                        .genres
                        .isEmpty()
                ) {
                    item {
                        EmptyStatsMessage(
                            text =
                            "No genre listening data is available for this range."
                        )
                    }
                } else {
                    currentSummary
                        .genres
                        .forEach { stat ->
                            item(
                                key =
                                "genre_${stat.name}"
                            ) {
                                GenreStatRow(
                                    stat = stat
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun StatsRangeSelector(
    selectedRange: ListeningStatsRange,
    onRangeSelected: (ListeningStatsRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 10.dp,
                vertical = 10.dp
            ),
        horizontalArrangement =
        Arrangement.spacedBy(
            7.dp
        )
    ) {
        ListeningStatsRange
            .values()
            .forEach { range ->
                val selected =
                    range == selectedRange

                Card(
                    modifier = Modifier
                        .widthIn(
                            min = 82.dp
                        )
                        .heightIn(
                            min = 40.dp
                        )
                        .auralArcClickable {
                            onRangeSelected(
                                range
                            )
                        },
                    shape =
                    RoundedCornerShape(
                        50
                    ),
                    backgroundColor =
                    if (
                        selected
                    ) {
                        AuralArcStyle.PurpleDark
                    } else {
                        AuralArcStyle.Surface
                    },
                    elevation =
                    if (
                        selected
                    ) {
                        6.dp
                    } else {
                        0.dp
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 10.dp
                        ),
                        contentAlignment =
                        Alignment.Center
                    ) {
                        Text(
                            text =
                            range.displayName,
                            style =
                            MaterialTheme
                                .typography
                                .caption,
                            fontWeight =
                            if (
                                selected
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                            color =
                            if (
                                selected
                            ) {
                                AuralArcStyle.TextPrimary
                            } else {
                                AuralArcStyle.TextMuted
                            },
                            maxLines = 1
                        )
                    }
                }
            }
    }
}

@Composable
private fun StatsSummaryCard(
    summary: ListeningStatsSummary
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 2.dp
            ),
        shape =
        AuralArcStyle.CardShape,
        backgroundColor =
        AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text =
                "Overview • ${summary.range.displayName}",
                style =
                MaterialTheme.typography.h6,
                fontWeight =
                FontWeight.Bold,
                color =
                AuralArcStyle.TextPrimary
            )

            StatLine(
                label =
                "Total listening time",
                value =
                formatStatsDuration(
                    summary.totalListeningMillis
                )
            )

            StatLine(
                label =
                "Counted plays",
                value =
                summary.totalPlays.toString()
            )

            StatLine(
                label =
                "Completed plays",
                value =
                summary.totalCompleted.toString()
            )

            StatLine(
                label =
                "Skips",
                value =
                summary.totalSkips.toString()
            )

            StatLine(
                label =
                "Unique tracks",
                value =
                summary.uniqueTracks.toString()
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        style =
        MaterialTheme.typography.subtitle1,
        fontWeight =
        FontWeight.Bold,
        color =
        AuralArcStyle.TextPrimary,
        modifier = Modifier.padding(
            start = 14.dp,
            end = 14.dp,
            top = 14.dp,
            bottom = 4.dp
        )
    )
}

@Composable
private fun TrackStatRow(
    stat: TrackListeningStats
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
        shape =
        AuralArcStyle.CardShape,
        backgroundColor =
        AuralArcStyle.Surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(
                12.dp
            )
        ) {
            Text(
                text =
                stat.title,
                fontWeight =
                FontWeight.Bold,
                color =
                AuralArcStyle.TextPrimary,
                maxLines = 1,
                overflow =
                TextOverflow.Ellipsis
            )

            Text(
                text =
                "${stat.artist} • ${stat.album}",
                style =
                MaterialTheme.typography.body2,
                color =
                AuralArcStyle.TextSecondary,
                maxLines = 1,
                overflow =
                TextOverflow.Ellipsis
            )

            Text(
                text =
                "${stat.playCount} plays • " +
                        "${formatStatsDuration(stat.listeningMillis)} listened • " +
                        "${stat.completedCount} completed • " +
                        "${stat.skipCount} skips",
                style =
                MaterialTheme.typography.caption,
                color =
                AuralArcStyle.TextMuted,
                maxLines = 2,
                overflow =
                TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GroupStatRow(
    stat: GroupListeningStats
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
        shape =
        AuralArcStyle.CardShape,
        backgroundColor =
        AuralArcStyle.Surface,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    12.dp
                ),
            verticalAlignment =
            Alignment.CenterVertically
        ) {
            Text(
                text =
                stat.name,
                color =
                AuralArcStyle.TextPrimary,
                fontWeight =
                FontWeight.Bold,
                maxLines = 1,
                overflow =
                TextOverflow.Ellipsis,
                modifier = Modifier.weight(
                    1f
                )
            )

            Spacer(
                modifier = Modifier.width(
                    10.dp
                )
            )

            Text(
                text =
                "${stat.playCount} plays • " +
                        formatStatsDuration(
                            stat.listeningMillis
                        ),
                style =
                MaterialTheme.typography.caption,
                color =
                AuralArcStyle.TextMuted
            )
        }
    }
}

@Composable
private fun GenreStatRow(
    stat: GenreListeningStats
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),
        shape =
        AuralArcStyle.CardShape,
        backgroundColor =
        AuralArcStyle.Surface,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    12.dp
                )
        ) {
            Row(
                modifier =
                Modifier.fillMaxWidth(),
                verticalAlignment =
                Alignment.CenterVertically
            ) {
                Text(
                    text =
                    stat.name,
                    color =
                    AuralArcStyle.TextPrimary,
                    fontWeight =
                    FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                    TextOverflow.Ellipsis,
                    modifier = Modifier.weight(
                        1f
                    )
                )

                Text(
                    text =
                    String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        stat.percentage
                    ),
                    style =
                    MaterialTheme.typography.body2,
                    fontWeight =
                    FontWeight.Bold,
                    color =
                    AuralArcStyle.TextPrimary
                )
            }

            Spacer(
                modifier = Modifier.height(
                    7.dp
                )
            )

            LinearProgressIndicator(
                progress =
                (
                        stat.percentage /
                                100f
                        ).coerceIn(
                        0f,
                        1f
                    ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        6.dp
                    ),
                color =
                AuralArcStyle.PurpleBright,
                backgroundColor =
                AuralArcStyle.SurfaceBright
            )

            Spacer(
                modifier = Modifier.height(
                    5.dp
                )
            )

            Text(
                text =
                formatStatsDuration(
                    stat.listeningMillis
                ),
                style =
                MaterialTheme.typography.caption,
                color =
                AuralArcStyle.TextMuted
            )
        }
    }
}

@Composable
private fun EmptyStatsMessage(
    text: String
) {
    Text(
        text = text,
        style =
        MaterialTheme.typography.body2,
        color =
        AuralArcStyle.TextMuted,
        modifier = Modifier.padding(
            14.dp
        )
    )
}

@Composable
private fun StatLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 7.dp
            )
    ) {
        Text(
            text =
            label,
            color =
            AuralArcStyle.TextSecondary,
            modifier = Modifier.weight(
                1f
            )
        )

        Text(
            text =
            value,
            color =
            AuralArcStyle.TextPrimary,
            fontWeight =
            FontWeight.Bold
        )
    }
}

private fun emptyRangeMessage(
    range: ListeningStatsRange
): String {
    return when (
        range
    ) {
        ListeningStatsRange.ALL_TIME ->
            "No listening stats yet. Play some music first."

        ListeningStatsRange.WEEKLY ->
            "No listening activity was recorded during the past 7 days."

        ListeningStatsRange.MONTHLY ->
            "No listening activity was recorded during the past 30 days."

        ListeningStatsRange.YEARLY ->
            "No listening activity was recorded during the past 365 days."
    }
}

private fun formatStatsDuration(
    millis: Long
): String {
    val totalSeconds =
        millis / 1000L

    val hours =
        totalSeconds / 3600L

    val minutes =
        (
                totalSeconds %
                        3600L
                ) / 60L

    val seconds =
        totalSeconds % 60L

    return when {
        hours > 0L ->
            "${hours}h ${minutes}m"

        minutes > 0L ->
            "${minutes}m ${seconds}s"

        else ->
            "${seconds}s"
    }
}