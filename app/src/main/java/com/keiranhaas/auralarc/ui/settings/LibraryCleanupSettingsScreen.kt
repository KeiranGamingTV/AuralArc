package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import com.keiranhaas.auralarc.ui.components.AuralArcBackButton
import com.keiranhaas.auralarc.ui.components.AuralArcTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.storage.LibraryCleanupPreferences
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.components.AuralArcSwitch
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

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
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            brush = AuralArcStyle.appBackgroundBrush()
        ),
        topBar = {
            AuralArcTopBar(
                title = "Library Cleanup",
                navigationIcon = {
                    AuralArcBackButton(
                        onClick = {
                            navController.safePopBackStack()
                        }
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

            AuralArcCard(
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AuralArcStyle.TextPrimary
                    )

                    Text(
                        text = "Used when Hide Short Tracks is enabled.",
                        style = MaterialTheme.typography.bodyMedium,
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
    AuralArcCard(
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 3.dp
                    )
                )
            }

            AuralArcSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}