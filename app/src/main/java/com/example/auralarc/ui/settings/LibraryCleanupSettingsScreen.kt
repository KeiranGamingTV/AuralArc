package com.example.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.auralarc.storage.LibraryCleanupPreferences
import com.example.auralarc.ui.theme.AuralArcStyle

@Composable
fun LibraryCleanupSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var hideUnknownArtist by remember {
        mutableStateOf(
            LibraryCleanupPreferences.getHideUnknownArtist(
                context
            )
        )
    }

    var hideShortTracks by remember {
        mutableStateOf(
            LibraryCleanupPreferences.getHideShortTracks(
                context
            )
        )
    }

    var minDuration by remember {
        mutableStateOf(
            LibraryCleanupPreferences.getMinDurationSeconds(
                context
            ).toString()
        )
    }

    var hideZeroDuration by remember {
        mutableStateOf(
            LibraryCleanupPreferences.getHideZeroDuration(
                context
            )
        )
    }

    var deduplicate by remember {
        mutableStateOf(
            LibraryCleanupPreferences.getDeduplicate(
                context
            )
        )
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
                        text = "Library Cleanup",
                        color = AuralArcStyle.TextPrimary
                    )
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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    bottom = 18.dp
                )
        ) {
            AuralArcMessageCard(
                title = "Cleanup Filters",
                message = "These filters apply the next time the library is refreshed."
            )

            CleanupToggleCard(
                title = "Hide Unknown Artists",
                subtitle = "Hide tracks where the artist is missing or Unknown Artist.",
                checked = hideUnknownArtist,
                onCheckedChange = { enabled ->
                    hideUnknownArtist =
                        enabled

                    LibraryCleanupPreferences.setHideUnknownArtist(
                        context,
                        enabled
                    )
                }
            )

            CleanupToggleCard(
                title = "Hide Zero-Duration Tracks",
                subtitle = "Hide tracks that report 0:00 duration.",
                checked = hideZeroDuration,
                onCheckedChange = { enabled ->
                    hideZeroDuration =
                        enabled

                    LibraryCleanupPreferences.setHideZeroDuration(
                        context,
                        enabled
                    )
                }
            )

            CleanupToggleCard(
                title = "Hide Short Tracks",
                subtitle = "Hide short sounds, samples, and notification-like audio files.",
                checked = hideShortTracks,
                onCheckedChange = { enabled ->
                    hideShortTracks =
                        enabled

                    LibraryCleanupPreferences.setHideShortTracks(
                        context,
                        enabled
                    )
                }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
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
                        text = "Minimum Track Length",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = AuralArcStyle.TextPrimary
                    )

                    Text(
                        text = "Used when Hide Short Tracks is enabled.",
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextMuted
                    )

                    OutlinedTextField(
                        value = minDuration,
                        onValueChange = { value ->
                            minDuration =
                                value.filter {
                                    it.isDigit()
                                }

                            val seconds =
                                minDuration.toIntOrNull()

                            if (
                                seconds != null
                            ) {
                                LibraryCleanupPreferences.setMinDurationSeconds(
                                    context,
                                    seconds
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Seconds"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 8.dp
                            )
                    )
                }
            }

            CleanupToggleCard(
                title = "Deduplicate Similar Tracks",
                subtitle = "Hide duplicates with the same title, artist, album, and duration.",
                checked = deduplicate,
                onCheckedChange = { enabled ->
                    deduplicate =
                        enabled

                    LibraryCleanupPreferences.setDeduplicate(
                        context,
                        enabled
                    )
                }
            )
        }
    }
}

@Composable
private fun CleanupToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 3.dp
                    )
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}