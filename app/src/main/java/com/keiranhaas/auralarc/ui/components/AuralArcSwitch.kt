package com.keiranhaas.auralarc.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun AuralArcSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Switch(
        checked =
            checked,

        onCheckedChange =
            onCheckedChange,

        enabled =
            enabled,

        colors =
            SwitchDefaults.colors(
                checkedThumbColor =
                    AuralArcStyle.TextPrimary,

                checkedTrackColor =
                    AuralArcStyle.Purple,

                checkedBorderColor =
                    AuralArcStyle.Purple,

                uncheckedThumbColor =
                    AuralArcStyle.TextSecondary,

                uncheckedTrackColor =
                    AuralArcStyle.SurfaceBright,

                uncheckedBorderColor =
                    AuralArcStyle.TextMuted,

                disabledCheckedThumbColor =
                    AuralArcStyle.TextMuted,

                disabledCheckedTrackColor =
                    AuralArcStyle.PurpleDark,

                disabledUncheckedThumbColor =
                    AuralArcStyle.TextMuted,

                disabledUncheckedTrackColor =
                    AuralArcStyle.Surface
            )
    )
}