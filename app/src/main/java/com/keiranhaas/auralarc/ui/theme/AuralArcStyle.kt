package com.keiranhaas.auralarc.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.storage.PowerSavingPreferences

object AuralArcStyle {

    /*
     * ------------------------------------------------------------------------
     * Core colors
     * ------------------------------------------------------------------------
     */

    private val powerSavingBlack: Boolean
        get() = PowerSavingPreferences.isEnabled()

    val BackgroundTop: Color
        get() = if (powerSavingBlack) {
            Color.Black
        } else {
            Color(0xFF140B24)
        }

    val BackgroundBottom: Color
        get() = if (powerSavingBlack) {
            Color.Black
        } else {
            Color(0xFF05020A)
        }

    val Surface: Color
        get() = if (powerSavingBlack) {
            Color(0xFF050505)
        } else {
            Color(0xFF171024)
        }

    val SurfaceBright: Color
        get() = if (powerSavingBlack) {
            Color(0xFF101010)
        } else {
            Color(0xFF211433)
        }

    val SurfaceSoft: Color
        get() = if (powerSavingBlack) {
            Color(0xFF181818)
        } else {
            Color(0xFF2C1A45)
        }

    val Purple =
        Color(0xFF9B5CFF)

    val PurpleBright =
        Color(0xFFC08BFF)

    val PurpleDark =
        Color(0xFF5A2EA6)

    val TextPrimary =
        Color(0xFFF8F2FF)

    val TextSecondary =
        Color(0xFFCDBDEB)

    val TextMuted =
        Color(0xFF9B8BB8)

    val Divider =
        Color(0xFF35224F)

    val Error =
        Color(0xFFFF6B8A)

    /*
     * ------------------------------------------------------------------------
     * Material 3 semantic surfaces
     * ------------------------------------------------------------------------
     *
     * These are intentionally separate from the original AuralArc colors.
     * This gives Material 3 components predictable semantic colors while
     * keeping the existing visual identity.
     */

    val SurfaceContainerLowest: Color
        get() = if (powerSavingBlack) {
            Color.Black
        } else {
            Color(0xFF0B0612)
        }

    val SurfaceContainerLow: Color
        get() = if (powerSavingBlack) {
            Color(0xFF050505)
        } else {
            Color(0xFF120B1D)
        }

    val SurfaceContainer: Color
        get() = Surface

    val SurfaceContainerHigh: Color
        get() = SurfaceBright

    val SurfaceContainerHighest: Color
        get() = if (powerSavingBlack) {
            Color(0xFF181818)
        } else {
            Color(0xFF2C1A45)
        }

    /*
     * ------------------------------------------------------------------------
     * Component shapes
     * ------------------------------------------------------------------------
     */

    val ExtraSmallShape =
        RoundedCornerShape(8.dp)

    val SmallShape =
        RoundedCornerShape(14.dp)

    val CardShape =
        RoundedCornerShape(22.dp)

    val LargeShape =
        RoundedCornerShape(28.dp)

    val ArtworkShape =
        RoundedCornerShape(18.dp)

    /*
     * ------------------------------------------------------------------------
     * Common dimensions
     * ------------------------------------------------------------------------
     */

    val CardPadding =
        16.dp

    /*
     * ------------------------------------------------------------------------
     * Backgrounds
     * ------------------------------------------------------------------------
     */

    fun appBackgroundBrush(): Brush {
        if (PowerSavingPreferences.isEnabled()) {
            return Brush.verticalGradient(
                colors = listOf(
                    Color.Black,
                    Color.Black
                )
            )
        }

        return Brush.verticalGradient(
            colors = listOf(
                BackgroundTop,
                BackgroundBottom
            )
        )
    }
}