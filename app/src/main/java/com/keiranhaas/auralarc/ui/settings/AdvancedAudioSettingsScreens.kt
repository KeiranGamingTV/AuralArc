package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.navigation.Screen
import com.keiranhaas.auralarc.storage.AdvancedAudioPreferences
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlin.math.roundToInt

@Composable
fun AdvancedAudioMenuScreen(
    navController: NavHostController
) {
    AdvancedAudioScaffold(
        title = "Audio",
        navController = navController
    ) {
        AudioMenuRow(
            title = "Audio Info",
            subtitle = "Detailed info about the current track and audio processing.",
            icon = Icons.Default.Info,
            onClick = {
                navController.navigate(
                    Screen.AudioInfoSettings.route
                )
            }
        )

        AudioMenuRow(
            title = "Audio Behavior",
            subtitle = "Smart Shuffle, app-close behavior, headphone disconnect, and basic playback behavior.",
            icon = Icons.Default.Settings,
            onClick = {
                navController.navigate(
                    Screen.AudioBehaviorSettings.route
                )
            }
        )

        AudioMenuRow(
            title = "Audio Focus",
            subtitle = "Pause, resume, or duck volume for calls and notifications.",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = {
                navController.navigate(
                    Screen.AudioFocusSettings.route
                )
            }
        )

        AudioMenuRow(
            title = "Direct Volume Control",
            subtitle = "DVC options and Bluetooth absolute-volume behavior.",
            icon = Icons.Default.SurroundSound,
            onClick = {
                navController.navigate(
                    Screen.DirectVolumeControlSettings.route
                )
            }
        )
    }
}

@Composable
fun AudioInfoSettingsScreen(
    navController: NavHostController
) {
    AdvancedAudioScaffold(
        title = "Audio Info",
        navController = navController
    ) {
        AudioInfoNotice(
            title = "Audio Info",
            message = "The detailed per-track Audio Info screen is opened from More Options on a song. This settings page is for explaining what the info means."
        )

        AudioInfoNotice(
            title = "What AuralArc Can Show",
            message = "AuralArc can show codec, MIME type, bitrate, sample rate, bit depth, channels, file size, source, ReplayGain metadata, and extractor details when Android exposes them."
        )

        AudioInfoNotice(
            title = "What Android May Hide",
            message = "Bluetooth codec, true DAC output, final Android mixer sample rate, and bit-perfect status are not always exposed to normal apps."
        )
    }
}

@Composable
fun AudioFocusSettingsScreen(
    navController: NavHostController
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    var resumeAfterCall by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getResumeAfterCall(
                context
            )
        )
    }

    var resumeOnFocusGain by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getResumeOnFocusGain(
                context
            )
        )
    }

    var duckVolume by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getDuckVolume(
                context
            )
        )
    }

    var permanentFocusChange by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getPermanentAudioFocusChange(
                context
            )
        )
    }

    AdvancedAudioScaffold(
        title = "Audio Focus",
        navController = navController
    ) {
        SwitchSetting(
            title = "Resume After Call",
            subtitle = "Resume playing on hang up if paused by call.",
            checked = resumeAfterCall,
            onCheckedChange = {
                resumeAfterCall =
                    it

                AdvancedAudioPreferences.setResumeAfterCall(
                    context,
                    it
                )
            }
        )

        SwitchSetting(
            title = "Resume On Focus Gain",
            subtitle = "Resume after getting audio focus back. If disabled, the player stays paused.",
            checked = resumeOnFocusGain,
            onCheckedChange = {
                resumeOnFocusGain =
                    it

                AdvancedAudioPreferences.setResumeOnFocusGain(
                    context,
                    it
                )
            }
        )

        SwitchSetting(
            title = "Duck Volume",
            subtitle = "Lower volume temporarily during short audio-focus changes.",
            checked = duckVolume,
            onCheckedChange = {
                duckVolume =
                    it

                AdvancedAudioPreferences.setDuckVolume(
                    context,
                    it
                )
            }
        )

        SwitchSetting(
            title = "Permanent Audio Focus Change",
            subtitle = "Pause on permanent audio focus changes from another player, app, or game.",
            checked = permanentFocusChange,
            onCheckedChange = {
                permanentFocusChange =
                    it

                AdvancedAudioPreferences.setPermanentAudioFocusChange(
                    context,
                    it
                )
            }
        )
    }
}

@Composable
fun DirectVolumeControlSettingsScreen(
    navController: NavHostController
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    var dvcEnabled by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getDvcEnabled(
                context
            )
        )
    }

    var noDvcBluetooth by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getNoDvcForBluetoothAbsoluteVolume(
                context
            )
        )
    }

    var preampReduction by remember {
        mutableStateOf(
            AdvancedAudioPreferences.getDvcPreampReduction(
                context
            )
        )
    }

    AdvancedAudioScaffold(
        title = "Direct Volume Control",
        navController = navController
    ) {
        AudioInfoNotice(
            title = "Direct Volume Control",
            message = "DVC-style behavior improves volume and EQ headroom in players with a custom audio engine. AuralArc will save these preferences now, then connect safe behavior later."
        )

        SwitchSetting(
            title = "Enable Direct Volume Control",
            subtitle = "Use DVC-style output handling when supported.",
            checked = dvcEnabled,
            onCheckedChange = {
                dvcEnabled =
                    it

                AdvancedAudioPreferences.setDvcEnabled(
                    context,
                    it
                )
            }
        )

        SwitchSetting(
            title = "No DVC for Bluetooth Absolute Volume",
            subtitle = "Disable DVC-style behavior for Bluetooth absolute volume devices.",
            checked = noDvcBluetooth,
            onCheckedChange = {
                noDvcBluetooth =
                    it

                AdvancedAudioPreferences.setNoDvcForBluetoothAbsoluteVolume(
                    context,
                    it
                )
            }
        )

        SliderSetting(
            title = "No DVC - Preamp Reduction",
            subtitle = "${preampReduction.roundToInt()} dB",
            value = preampReduction,
            valueRange = -12f..0f,
            onValueChange = {
                preampReduction =
                    it

                AdvancedAudioPreferences.setDvcPreampReduction(
                    context,
                    it
                )
            }
        )
    }
}

@Composable
fun AdvancedAudioScaffold(
    title: String,
    navController: NavHostController,
    content: @Composable ColumnScope.() -> Unit
) {
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
                        text = title,
                        color = AuralArcStyle.TextPrimary,
                        fontWeight = FontWeight.Bold
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
                    bottom = 24.dp
                )
        ) {
            content()
        }
    }
}

@Composable
private fun AudioMenuRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(
                14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuralArcStyle.PurpleBright
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(
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
                    color = AuralArcStyle.TextMuted
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
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
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted
            )

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange
            )
        }
    }
}

@Composable
private fun AudioInfoNotice(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
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
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    top = 4.dp
                )
            )
        }
    }
}