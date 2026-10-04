package com.keiranhaas.auralarc.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun AuralArcCard(
    modifier: Modifier = Modifier,
    shape: Shape =
        AuralArcStyle.CardShape,
    backgroundColor: Color =
        AuralArcStyle.Surface,
    elevation: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier =
            modifier,

        shape =
            shape,

        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    elevation
            ),

        content =
            content
    )
}