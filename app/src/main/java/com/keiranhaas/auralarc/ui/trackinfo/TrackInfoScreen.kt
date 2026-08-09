package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import com.keiranhaas.auralarc.utils.audioDetailRows
import com.keiranhaas.auralarc.utils.audioQualitySummary
import com.keiranhaas.auralarc.utils.musicMetadataRows
import com.keiranhaas.auralarc.utils.replayGainRows
import com.keiranhaas.auralarc.utils.sourceAndFileRows
import com.keiranhaas.auralarc.utils.technicalAudioRows
import com.keiranhaas.auralarc.player.AudioProcessingState
import android.app.Activity
import android.app.PendingIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.keiranhaas.auralarc.storage.AudioTagEditResult
import com.keiranhaas.auralarc.storage.AudioTagEditor
import com.keiranhaas.auralarc.storage.EditableTrackMetadata
import kotlinx.coroutines.launch

@Composable
fun TrackInfoScreen(
    track: MusicTrack,
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var showMetadataEditor by remember(
        track.uri
    ) {
        mutableStateOf(
            false
        )
    }

    var metadataSaveInProgress by remember {
        mutableStateOf(
            false
        )
    }

    var editResultMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var pendingMetadata by remember {
        mutableStateOf<EditableTrackMetadata?>(
            null
        )
    }

    var pendingWritePermission by remember {
        mutableStateOf<PendingIntent?>(
            null
        )
    }

    suspend fun performMetadataWrite(
        metadata: EditableTrackMetadata
    ) {
        metadataSaveInProgress =
            true

        when (
            val result =
                AudioTagEditor.updateTrackMetadata(
                    context =
                    context.applicationContext,
                    track = track,
                    metadata = metadata
                )
        ) {
            is AudioTagEditResult.Success -> {
                applyUpdatedAudioTracks(
                    context =
                    context.applicationContext,
                    updatedTracks =
                    result.updatedTracks
                )

                showMetadataEditor =
                    false

                pendingMetadata =
                    null

                editResultMessage =
                    result.message
            }

            is AudioTagEditResult.PermissionRequired -> {
                pendingWritePermission =
                    result.pendingIntent
            }

            is AudioTagEditResult.Failure -> {
                editResultMessage =
                    result.message
            }
        }

        metadataSaveInProgress =
            false
    }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
            ActivityResultContracts
                .StartIntentSenderForResult()
        ) { activityResult ->
            if (
                activityResult.resultCode ==
                Activity.RESULT_OK
            ) {
                val metadata =
                    pendingMetadata

                if (
                    metadata != null
                ) {
                    scope.launch {
                        performMetadataWrite(
                            metadata
                        )
                    }
                }
            } else {
                pendingMetadata =
                    null

                editResultMessage =
                    "Write permission was not granted."
            }
        }

    LaunchedEffect(
        pendingWritePermission
    ) {
        val request =
            pendingWritePermission
                ?: return@LaunchedEffect

        pendingWritePermission =
            null

        writePermissionLauncher.launch(
            IntentSenderRequest
                .Builder(
                    request.intentSender
                )
                .build()
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
                        text = "Audio Info",
                        color = AuralArcStyle.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    AuralArcIconButton(
                        enabled =
                        !metadataSaveInProgress,
                        onClick = {
                            if (
                                AudioTagEditor.isTrackEditable(
                                    track
                                )
                            ) {
                                showMetadataEditor =
                                    true
                            } else {
                                editResultMessage =
                                    AudioTagEditor.readOnlyReason(
                                        track
                                    )
                            }
                        }
                    ) {
                        Icon(
                            imageVector =
                            Icons.Default.Edit,
                            contentDescription =
                            "Edit song metadata",
                            tint =
                            if (
                                AudioTagEditor.isTrackEditable(
                                    track
                                )
                            ) {
                                AuralArcStyle.TextPrimary
                            } else {
                                AuralArcStyle.TextMuted
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .padding(
                    bottom = 18.dp
                ),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 24.dp
            )
        ) {
            item {
                TrackInfoHeaderCard(
                    track = track
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "Audio Quality",
                    subtitle = "Readable format, HD status, codec, sample rate, bit depth, bitrate, and channel layout.",
                    icon = Icons.Default.GraphicEq,
                    rows = audioDetailRows(
                        track
                    )
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "Music Metadata",
                    subtitle = "Tags and library metadata used for browsing, sorting, and display.",
                    icon = Icons.Default.Audiotrack,
                    rows = musicMetadataRows(
                        track
                    )
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "File / Source",
                    subtitle = "Where the track came from and what file/container information AuralArc could read.",
                    icon = Icons.Default.Storage,
                    rows = sourceAndFileRows(
                        track
                    ),
                    monospaceValues = true
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "Technical Stream Details",
                    subtitle = "Low-level MediaExtractor details. Some formats or remote streams may not expose every field.",
                    icon = Icons.Default.Memory,
                    rows = technicalAudioRows(
                        track
                    )
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "ReplayGain / Loudness Metadata",
                    subtitle = "ReplayGain values if they are provided by the file, library, or Navidrome.",
                    icon = Icons.Default.Speed,
                    rows = replayGainRows(
                        track
                    )
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "Playback / Processing Notes",
                    subtitle = "Important limits about what this screen can and cannot prove about final output.",
                    icon = Icons.Default.Info,
                    rows = listOf(
                        "Track Info Scope" to "This screen describes the audio file or stream metadata that AuralArc can read.",
                        "ReplayGain" to AudioProcessingState.replayGainLabel.value,
                        "ReplayGain Source" to AudioProcessingState.replayGainSourceLabel.value,
                        "ReplayGain Multiplier" to "%.4f".format(
                            AudioProcessingState.replayGainMultiplier.value
                        ),
                        "Saved Resampler" to AudioProcessingState.resamplerLabel.value,
                        "Saved Dither" to AudioProcessingState.ditherLabel.value,
                        "Saved DVC" to AudioProcessingState.dvcLabel.value,
                        "Saved Output Plugin" to AudioProcessingState.outputPluginLabel.value,
                        "Bluetooth Codec" to "Not shown here. Android does not reliably expose the active Bluetooth codec to regular media apps.",
                        "DAC Output" to "Not guaranteed. Android, Bluetooth, or device audio processing may resample after AuralArc reads the file.",
                        "Bit-Perfect Status" to "Not guaranteed. This requires device/output-path verification beyond normal track metadata.",
                        "HD Badge Rule" to "Shown when the track reports 24-bit or higher bit depth.",
                        "Metadata Warning" to track.metadataWarning.ifBlank {
                            "No metadata warnings reported."
                        }
                    )
                )
            }

            item {
                TrackInfoSectionCard(
                    title = "Raw Debug Values",
                    subtitle = "Raw values stored in the MusicTrack object. Useful for debugging scanner and Navidrome metadata.",
                    icon = Icons.Default.Code,
                    rows = listOf(
                        "id" to track.id.toString(),
                        "uri" to track.uri,
                        "albumId" to track.albumId.toString(),
                        "trackNumber" to track.trackNumber.toString(),
                        "discNumber" to track.discNumber.toString(),
                        "releaseYear" to track.releaseYear.toString(),
                        "duration" to track.duration.toString(),
                        "audioCodec" to track.audioCodec,
                        "mimeType" to track.mimeType,
                        "bitrate" to track.bitrate.toString(),
                        "sampleRate" to track.sampleRate.toString(),
                        "bitDepth" to track.bitDepth.toString(),
                        "channelCount" to track.channelCount.toString(),
                        "fileSizeBytes" to track.fileSizeBytes.toString(),
                        "dateAddedMillis" to track.dateAddedMillis.toString(),
                        "dateModifiedMillis" to track.dateModifiedMillis.toString(),
                        "container" to track.container,
                        "fileExtension" to track.fileExtension,
                        "sourceType" to track.sourceType,
                        "sourcePath" to track.sourcePath,
                        "lossless" to track.lossless.toString(),
                        "audioTrackCount" to track.audioTrackCount.toString(),
                        "extractorTrackCount" to track.extractorTrackCount.toString(),
                        "audioTrackIndex" to track.audioTrackIndex.toString(),
                        "pcmEncoding" to track.pcmEncoding.toString(),
                        "maxInputSize" to track.maxInputSize.toString(),
                        "encoderDelay" to track.encoderDelay.toString(),
                        "encoderPadding" to track.encoderPadding.toString(),
                        "language" to track.language,
                        "profile" to track.profile.toString(),
                        "level" to track.level.toString()
                    ),
                    monospaceValues = true
                )
            }
        }
    }

    if (
        showMetadataEditor
    ) {
        MetadataEditDialog(
            track = track,
            saving =
            metadataSaveInProgress,
            onDismiss = {
                if (
                    !metadataSaveInProgress
                ) {
                    showMetadataEditor =
                        false
                }
            },
            onSave = { metadata ->
                pendingMetadata =
                    metadata

                val permissionRequest =
                    AudioTagEditor.createWriteRequest(
                        context = context,
                        tracks =
                        listOf(
                            track
                        )
                    )

                if (
                    permissionRequest != null
                ) {
                    pendingWritePermission =
                        permissionRequest
                } else {
                    scope.launch {
                        performMetadataWrite(
                            metadata
                        )
                    }
                }
            }
        )
    }

    editResultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                editResultMessage =
                    null
            },
            title = {
                Text(
                    text = "Metadata Editor"
                )
            },
            text = {
                Text(
                    text = message
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editResultMessage =
                            null
                    }
                ) {
                    Text(
                        text = "OK"
                    )
                }
            }
        )
    }
}

@Composable
private fun MetadataEditDialog(
    track: MusicTrack,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (EditableTrackMetadata) -> Unit
) {
    val original =
        remember(
            track
        ) {
            AudioTagEditor.metadataFromTrack(
                track
            )
        }

    var title by remember(
        track.uri,
        track.title
    ) {
        mutableStateOf(
            original.title
        )
    }

    var artist by remember(
        track.uri,
        track.artist
    ) {
        mutableStateOf(
            original.artist
        )
    }

    var album by remember(
        track.uri,
        track.album
    ) {
        mutableStateOf(
            original.album
        )
    }

    var albumArtist by remember(
        track.uri,
        track.albumArtist
    ) {
        mutableStateOf(
            original.albumArtist
        )
    }

    var genre by remember(
        track.uri,
        track.genre
    ) {
        mutableStateOf(
            original.genre
        )
    }

    var yearText by remember(
        track.uri,
        track.releaseYear
    ) {
        mutableStateOf(
            original.year
                ?.toString()
                .orEmpty()
        )
    }

    var trackNumberText by remember(
        track.uri,
        track.trackNumber
    ) {
        mutableStateOf(
            original.trackNumber
                ?.toString()
                .orEmpty()
        )
    }

    var discNumberText by remember(
        track.uri,
        track.discNumber
    ) {
        mutableStateOf(
            original.discNumber
                ?.toString()
                .orEmpty()
        )
    }

    var validationMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    fun parseOptionalNumber(
        rawValue: String,
        label: String,
        maximum: Int
    ): Int? {
        val trimmed =
            rawValue.trim()

        if (
            trimmed.isBlank()
        ) {
            return null
        }

        val value =
            trimmed.toIntOrNull()

        if (
            value == null ||
            value <= 0 ||
            value > maximum
        ) {
            validationMessage =
                "$label must be between 1 and $maximum."

            return Int.MIN_VALUE
        }

        return value
    }

    AlertDialog(
        onDismissRequest = {
            if (
                !saving
            ) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "Edit Song Metadata"
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(
                    max = 520.dp
                ),
                contentPadding =
                PaddingValues(
                    bottom = 4.dp
                )
            ) {
                item {
                    Text(
                        text = "AuralArc writes these values into the audio file itself. Unsupported tags are left unchanged.",
                        style =
                        MaterialTheme.typography.body2,
                        color =
                        AuralArcStyle.TextMuted
                    )

                    Spacer(
                        modifier =
                        Modifier.height(
                            10.dp
                        )
                    )
                }

                item {
                    MetadataTextField(
                        value = title,
                        onValueChange = {
                            title = it
                        },
                        label = "Title",
                        enabled = !saving
                    )
                }

                item {
                    MetadataTextField(
                        value = artist,
                        onValueChange = {
                            artist = it
                        },
                        label = "Artist",
                        enabled = !saving
                    )
                }

                item {
                    MetadataTextField(
                        value = album,
                        onValueChange = {
                            album = it
                        },
                        label = "Album",
                        enabled = !saving
                    )
                }

                item {
                    MetadataTextField(
                        value = albumArtist,
                        onValueChange = {
                            albumArtist = it
                        },
                        label = "Album Artist",
                        enabled = !saving
                    )
                }

                item {
                    MetadataTextField(
                        value = genre,
                        onValueChange = {
                            genre = it
                        },
                        label = "Genre",
                        enabled = !saving
                    )
                }

                item {
                    MetadataTextField(
                        value = yearText,
                        onValueChange = { value ->
                            yearText =
                                value.filter { character ->
                                    character.isDigit()
                                }.take(
                                    4
                                )
                        },
                        label = "Year",
                        enabled = !saving,
                        keyboardType =
                        KeyboardType.Number
                    )
                }

                item {
                    MetadataTextField(
                        value =
                        trackNumberText,
                        onValueChange = { value ->
                            trackNumberText =
                                value.filter { character ->
                                    character.isDigit()
                                }.take(
                                    4
                                )
                        },
                        label = "Track Number",
                        enabled = !saving,
                        keyboardType =
                        KeyboardType.Number
                    )
                }

                item {
                    MetadataTextField(
                        value =
                        discNumberText,
                        onValueChange = { value ->
                            discNumberText =
                                value.filter { character ->
                                    character.isDigit()
                                }.take(
                                    3
                                )
                        },
                        label = "Disc Number",
                        enabled = !saving,
                        keyboardType =
                        KeyboardType.Number
                    )
                }

                validationMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            style =
                            MaterialTheme.typography.body2,
                            color =
                            MaterialTheme.colors.error,
                            modifier =
                            Modifier.padding(
                                top = 6.dp
                            )
                        )
                    }
                }

                if (
                    saving
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 12.dp
                                ),
                            horizontalArrangement =
                            Arrangement.Center,
                            verticalAlignment =
                            Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier =
                                Modifier.size(
                                    22.dp
                                ),
                                strokeWidth = 2.dp,
                                color =
                                AuralArcStyle.PurpleBright
                            )

                            Spacer(
                                modifier =
                                Modifier.width(
                                    10.dp
                                )
                            )

                            Text(
                                text = "Saving metadata…",
                                color =
                                AuralArcStyle.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    validationMessage =
                        null

                    if (
                        title.isBlank()
                    ) {
                        validationMessage =
                            "The title cannot be blank."

                        return@TextButton
                    }

                    val year =
                        parseOptionalNumber(
                            rawValue =
                            yearText,
                            label = "Year",
                            maximum = 9999
                        )

                    if (
                        year == Int.MIN_VALUE
                    ) {
                        return@TextButton
                    }

                    val trackNumber =
                        parseOptionalNumber(
                            rawValue =
                            trackNumberText,
                            label =
                            "Track number",
                            maximum = 9999
                        )

                    if (
                        trackNumber ==
                        Int.MIN_VALUE
                    ) {
                        return@TextButton
                    }

                    val discNumber =
                        parseOptionalNumber(
                            rawValue =
                            discNumberText,
                            label =
                            "Disc number",
                            maximum = 999
                        )

                    if (
                        discNumber ==
                        Int.MIN_VALUE
                    ) {
                        return@TextButton
                    }

                    onSave(
                        EditableTrackMetadata(
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtist =
                            albumArtist,
                            genre = genre,
                            year = year,
                            trackNumber =
                            trackNumber,
                            discNumber =
                            discNumber
                        )
                    )
                }
            ) {
                Text(
                    text =
                    if (
                        saving
                    ) {
                        "Saving…"
                    } else {
                        "Save"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel"
                )
            }
        }
    )
}

@Composable
private fun MetadataTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardType: KeyboardType =
        KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange =
        onValueChange,
        label = {
            Text(
                text = label
            )
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions =
        KeyboardOptions(
            keyboardType =
            keyboardType
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 4.dp
            )
    )
}

@Composable
private fun TrackInfoHeaderCard(
    track: MusicTrack
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
        elevation = 10.dp
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
                albumArtPath = track.albumArtPath,
                size = 96.dp
            )

            Spacer(
                modifier = Modifier.width(
                    14.dp
                )
            )

            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                TrackTitleWithHdBadge(
                    track = track,
                    style = MaterialTheme.typography.h6,
                    color = AuralArcStyle.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    fillTitleWeight = true
                )

                Spacer(
                    modifier = Modifier.height(
                        4.dp
                    )
                )

                Text(
                    text = track.artist.ifBlank {
                        "Unknown Artist"
                    },
                    style = MaterialTheme.typography.body1,
                    color = AuralArcStyle.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.album.ifBlank {
                        "Unknown Album"
                    },
                    style = MaterialTheme.typography.body2,
                    color = AuralArcStyle.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(
                        8.dp
                    )
                )

                Text(
                    text = audioQualitySummary(
                        track
                    ),
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.PurpleBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrackInfoSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    rows: List<Pair<String, String>>,
    monospaceValues: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AuralArcStyle.PurpleBright
                )

                Spacer(
                    modifier = Modifier.width(
                        8.dp
                    )
                )

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
                        style = MaterialTheme.typography.caption,
                        color = AuralArcStyle.TextMuted
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(
                    10.dp
                )
            )

            TrackInfoDivider()

            rows.forEachIndexed { index, row ->
                TrackInfoRow(
                    label = row.first,
                    value = emptyToUnavailable(
                        row.second
                    ),
                    monospaceValue = monospaceValues
                )

                if (
                    index != rows.lastIndex
                ) {
                    TrackInfoDivider()
                }
            }
        }
    }
}

@Composable
private fun TrackInfoRow(
    label: String,
    value: String,
    monospaceValue: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 8.dp
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = AuralArcStyle.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value,
            style =
            if (
                monospaceValue
            ) {
                monoTextStyle()
            } else {
                MaterialTheme.typography.body2
            },
            color = AuralArcStyle.TextPrimary
        )
    }
}

@Composable
private fun TrackInfoDivider() {
    Divider(
        color = AuralArcStyle.TextMuted.copy(
            alpha = 0.18f
        ),
        thickness = 1.dp
    )
}

@Composable
private fun monoTextStyle(): TextStyle {
    return MaterialTheme.typography.body2.copy(
        fontFamily = FontFamily.Monospace
    )
}

private fun emptyToUnavailable(
    value: String
): String {
    return value.ifBlank {
        "Unavailable"
    }
}