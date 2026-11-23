package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.player.CrossfadeManager
import com.keiranhaas.auralarc.storage.CrossfadePreferences
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlin.math.roundToInt

@Composable
fun CrossfadeSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var enabled by remember {
        mutableStateOf(
            CrossfadePreferences.getEnabled(
                context
            )
        )
    }

    var mode by remember {
        mutableIntStateOf(
            CrossfadePreferences.getMode(
                context
            )
        )
    }

    var seconds by remember {
        mutableIntStateOf(
            CrossfadePreferences.getSeconds(
                context
            )
        )
    }

    AdvancedAudioScaffold(
        title = "Crossfade",
        navController = navController
    ) {
        SwitchSetting(
            title = "Crossfade",
            subtitle = "When off, songs play back to back with no gap.",
            checked = enabled,
            onCheckedChange = {
                enabled =
                    it

                CrossfadePreferences.setEnabled(
                    context,
                    it
                )

                CrossfadeManager.sync(
                    context
                )
            }
        )

        if (
            enabled
        ) {
            CrossfadeModeRow(
                title = "Overlap",
                subtitle = "The next song starts while the current one fades out. The fading song plays without the equalizer and effects.",
                selected = mode == CrossfadePreferences.MODE_OVERLAP,
                onClick = {
                    mode =
                        CrossfadePreferences.MODE_OVERLAP

                    CrossfadePreferences.setMode(
                        context,
                        CrossfadePreferences.MODE_OVERLAP
                    )

                    CrossfadeManager.cancelAll()
                }
            )

            CrossfadeModeRow(
                title = "Fade out and in",
                subtitle = "The current song fades out, then the next song fades in.",
                selected = mode == CrossfadePreferences.MODE_FADE,
                onClick = {
                    mode =
                        CrossfadePreferences.MODE_FADE

                    CrossfadePreferences.setMode(
                        context,
                        CrossfadePreferences.MODE_FADE
                    )

                    CrossfadeManager.cancelAll()
                }
            )

            SliderSetting(
                title = "Fade Length",
                subtitle = "$seconds seconds",
                value = seconds.toFloat(),
                valueRange = 1f..12f,
                onValueChange = {
                    seconds =
                        it.roundToInt()

                    CrossfadePreferences.setSeconds(
                        context,
                        seconds
                    )
                }
            )
        }
    }
}

@Composable
private fun CrossfadeModeRow(
    title: String,
    subtitle: String,
    selected: Boolean,
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
        backgroundColor =
            AuralArcStyle.Surface
    ) {
        Row(
            modifier = Modifier.padding(
                14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )

            Column(
                modifier = Modifier
                    .weight(
                        1f
                    )
                    .padding(
                        start = 14.dp
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
        }
    }
}
