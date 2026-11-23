package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.player.EqualizerManager
import com.keiranhaas.auralarc.player.EqualizerPresets
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.components.AuralArcSlider
import com.keiranhaas.auralarc.ui.components.AuralArcSwitch
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun EqualizerSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    LaunchedEffect(Unit) {
        PlayerManager.getPlayer(
            context
        )

        PlayerManager.attachEqualizerToCurrentAudioSession(
            context
        )
    }

    val bands =
        EqualizerManager.availableBandsHz

    val gains =
        EqualizerManager.bandGainsDb

    val activePreset =
        EqualizerManager.matchingPresetName()

    AdvancedAudioScaffold(
        title = "Equalizer",
        navController = navController
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
                        text = "Equalizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AuralArcStyle.TextPrimary
                    )

                    Text(
                        text = EqualizerManager.statusText.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuralArcStyle.TextMuted,
                        modifier = Modifier.padding(
                            top = 4.dp
                        )
                    )
                }

                AuralArcSwitch(
                    checked = EqualizerManager.enabled.value,
                    onCheckedChange = {
                        EqualizerManager.setEnabled(
                            context,
                            it
                        )
                    },
                    enabled = bands.isNotEmpty()
                )
            }
        }

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
                    text = "Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = "Current: $activePreset",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 10.dp
                    )
                )

                Row(
                    modifier = Modifier.horizontalScroll(
                        rememberScrollState()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp
                    )
                ) {
                    EqualizerPresets.all.forEach { preset ->
                        val selected =
                            preset.name == activePreset

                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        50
                                    )
                                )
                                .background(
                                    if (
                                        selected
                                    ) {
                                        AuralArcStyle.Purple
                                    } else {
                                        AuralArcStyle.Surface
                                    }
                                )
                                .auralArcClickable(
                                    enabled = bands.isNotEmpty()
                                ) {
                                    EqualizerManager.applyPreset(
                                        context,
                                        preset
                                    )
                                }
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 8.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (
                                    selected
                                ) {
                                    AuralArcStyle.TextPrimary
                                } else {
                                    AuralArcStyle.TextMuted
                                }
                            )
                        }
                    }
                }
            }
        }

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
                    text = "Bands",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                if (
                    bands.isEmpty() ||
                    gains.size != bands.size
                ) {
                    Text(
                        text = "Start playing a song to load the equalizer bands.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuralArcStyle.TextMuted,
                        modifier = Modifier.padding(
                            top = 4.dp
                        )
                    )
                } else {
                    bands.forEachIndexed { index, hz ->
                        Row(
                            modifier = Modifier.padding(
                                top = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatBandFrequency(
                                    hz
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuralArcStyle.TextMuted,
                                modifier = Modifier.width(
                                    64.dp
                                )
                            )

                            AuralArcSlider(
                                value = gains[index],
                                onValueChange = {
                                    EqualizerManager.setBandGain(
                                        context,
                                        index,
                                        (it * 2f).toInt() / 2f
                                    )
                                },
                                valueRange = -12f..12f,
                                modifier = Modifier.weight(
                                    1f
                                )
                            )

                            Text(
                                text = formatBandGain(
                                    gains[index]
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuralArcStyle.TextPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(
                                    64.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        AuralArcButton(
            onClick = {
                EqualizerManager.applyPreset(
                    context,
                    EqualizerPresets.flat
                )
            },
            enabled = bands.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                )
        ) {
            Text(
                text = "Reset to Flat"
            )
        }
    }
}

private fun formatBandFrequency(
    hz: Int
): String {
    return if (
        hz >= 1000
    ) {
        if (
            hz % 1000 == 0
        ) {
            "${hz / 1000} kHz"
        } else {
            "${"%.1f".format(hz / 1000f)} kHz"
        }
    } else {
        "$hz Hz"
    }
}

private fun formatBandGain(
    gain: Float
): String {
    return "${"%+.1f".format(gain)} dB"
}