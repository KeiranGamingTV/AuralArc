package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.keiranhaas.auralarc.ui.components.AuralArcBackButton
import com.keiranhaas.auralarc.ui.components.AuralArcTopBar
import androidx.compose.material.icons.Icons
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
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.components.AuralArcSlider
import com.keiranhaas.auralarc.ui.components.AuralArcSwitch
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import com.keiranhaas.auralarc.player.DirectVolumeControl

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
            message = "While music plays, Android's system volume is held at maximum and the volume keys control AuralArc's own volume instead. Your system volume is restored when playback pauses or a Bluetooth output is connected."
        )

        SwitchSetting(
            title = "Enable Direct Volume Control",
            subtitle = "Control volume inside AuralArc while music plays.",
            checked = dvcEnabled,
            onCheckedChange = {
                dvcEnabled =
                    it

                AdvancedAudioPreferences.setDvcEnabled(
                    context,
                    it
                )

                DirectVolumeControl.sync()
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

                DirectVolumeControl.sync()
            }
        )

        if (
            DirectVolumeControl.active.value
        ) {
            SliderSetting(
                title = "DVC Volume",
                subtitle = "${DirectVolumeControl.level.intValue}%",
                value = DirectVolumeControl.level.intValue.toFloat(),
                valueRange = 0f..100f,
                onValueChange = {
                    DirectVolumeControl.setLevel(
                        it.roundToInt()
                    )
                }
            )
        }

        SliderSetting(
            title = "No DVC - Preamp Reduction",
            subtitle = "${preampReduction.roundToInt()} dB, applied when DVC is off and the equalizer is on",
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
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            brush = AuralArcStyle.appBackgroundBrush()
        ),
        topBar = {
            AuralArcTopBar(
                title = title,
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
    AuralArcCard(
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuralArcStyle.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        top = 4.dp
                    )
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
    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
        backgroundColor =
            AuralArcStyle.Surface
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 4.dp
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

@Composable
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
        backgroundColor =
            AuralArcStyle.Surface
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
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
                    top = 4.dp,
                    bottom = 8.dp
                )
            )

            AuralArcSlider(
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
    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
        backgroundColor =
            AuralArcStyle.Surface
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    top = 4.dp
                )
            )
        }
    }
}