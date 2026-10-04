package com.keiranhaas.auralarc.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun AuralArcChoiceButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick =
            onClick,

        modifier =
            modifier,

        shape =
            AuralArcStyle.SmallShape,

        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor =
                    if (
                        selected
                    ) {
                        AuralArcStyle.PurpleDark
                    } else {
                        AuralArcStyle.Surface
                    },

                contentColor =
                    AuralArcStyle.TextPrimary,

                disabledContainerColor =
                    AuralArcStyle.SurfaceContainerLow,

                disabledContentColor =
                    AuralArcStyle.TextMuted
            ),

        content =
            content
    )
}