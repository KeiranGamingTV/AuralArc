package com.keiranhaas.auralarc.ui.components

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun AuralArcSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(
        value =
            value,

        onValueChange =
            onValueChange,

        valueRange =
            valueRange,

        modifier =
            modifier,

        enabled =
            enabled,

        steps =
            steps,

        onValueChangeFinished =
            onValueChangeFinished,

        colors =
            SliderDefaults.colors(
                thumbColor =
                    AuralArcStyle.PurpleBright,

                activeTrackColor =
                    AuralArcStyle.PurpleBright,

                inactiveTrackColor =
                    AuralArcStyle.SurfaceBright,

                activeTickColor =
                    AuralArcStyle.PurpleBright,

                inactiveTickColor =
                    AuralArcStyle.TextMuted,

                disabledThumbColor =
                    AuralArcStyle.TextMuted,

                disabledActiveTrackColor =
                    AuralArcStyle.PurpleDark,

                disabledInactiveTrackColor =
                    AuralArcStyle.Surface
            )
    )
}