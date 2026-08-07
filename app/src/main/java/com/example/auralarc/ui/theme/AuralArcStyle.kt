package com.example.auralarc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AuralArcStyle {
    val BackgroundTop = Color(0xFF140B24)
    val BackgroundBottom = Color(0xFF05020A)

    val Surface = Color(0xFF171024)
    val SurfaceBright = Color(0xFF211433)
    val SurfaceSoft = Color(0xFF2C1A45)

    val Purple = Color(0xFF9B5CFF)
    val PurpleBright = Color(0xFFC08BFF)
    val PurpleDark = Color(0xFF5A2EA6)

    val TextPrimary = Color(0xFFF8F2FF)
    val TextSecondary = Color(0xFFCDBDEB)
    val TextMuted = Color(0xFF9B8BB8)

    val Divider = Color(0xFF35224F)

    val CardShape = RoundedCornerShape(22.dp)
    val SmallShape = RoundedCornerShape(14.dp)
    val ArtworkShape = RoundedCornerShape(18.dp)

    fun appBackgroundBrush(): Brush {
        return Brush.verticalGradient(
            colors = listOf(
                BackgroundTop,
                BackgroundBottom
            )
        )
    }

    fun artworkBrush(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                PurpleBright,
                Purple,
                PurpleDark
            )
        )
    }
}