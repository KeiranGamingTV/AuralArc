package com.keiranhaas.auralarc.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.player.AudioEffectsManager
import com.keiranhaas.auralarc.player.PlayerManager
import kotlin.math.roundToInt

@Composable
fun SoundEffectsSettingsScreen(
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

    AdvancedAudioScaffold(
        title = "Sound Effects",
        navController = navController
    ) {
        SwitchSetting(
            title = "Bass Boost",
            subtitle = if (
                AudioEffectsManager.bassSupported.value
            ) {
                "Adds low-frequency punch."
            } else {
                "Not supported on this device."
            },
            checked = AudioEffectsManager.bassEnabled.value,
            onCheckedChange = {
                AudioEffectsManager.setBassEnabled(
                    context,
                    it
                )
            }
        )

        if (
            AudioEffectsManager.bassEnabled.value &&
            AudioEffectsManager.bassSupported.value
        ) {
            SliderSetting(
                title = "Bass Boost Strength",
                subtitle = "${AudioEffectsManager.bassStrength.value / 10}%",
                value = AudioEffectsManager.bassStrength.value.toFloat(),
                valueRange = 0f..1000f,
                onValueChange = {
                    AudioEffectsManager.setBassStrength(
                        context,
                        it.roundToInt()
                    )
                }
            )
        }

        SwitchSetting(
            title = "Virtualizer",
            subtitle = if (
                AudioEffectsManager.virtualizerSupported.value
            ) {
                "Widens the stereo image, best with headphones."
            } else {
                "Not supported on this device."
            },
            checked = AudioEffectsManager.virtualizerEnabled.value,
            onCheckedChange = {
                AudioEffectsManager.setVirtualizerEnabled(
                    context,
                    it
                )
            }
        )

        if (
            AudioEffectsManager.virtualizerEnabled.value &&
            AudioEffectsManager.virtualizerSupported.value
        ) {
            SliderSetting(
                title = "Virtualizer Strength",
                subtitle = "${AudioEffectsManager.virtualizerStrength.value / 10}%",
                value = AudioEffectsManager.virtualizerStrength.value.toFloat(),
                valueRange = 0f..1000f,
                onValueChange = {
                    AudioEffectsManager.setVirtualizerStrength(
                        context,
                        it.roundToInt()
                    )
                }
            )
        }

        SliderSetting(
            title = "Loudness Boost",
            subtitle = "${"%.1f".format(AudioEffectsManager.loudnessBoostDb.value)} dB, also applies positive ReplayGain",
            value = AudioEffectsManager.loudnessBoostDb.value,
            valueRange = 0f..12f,
            onValueChange = {
                AudioEffectsManager.setLoudnessBoostDb(
                    context,
                    (it * 2f).roundToInt() / 2f
                )
            }
        )
    }
}