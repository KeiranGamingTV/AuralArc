package com.keiranhaas.auralarc.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme as Material2Theme
import androidx.compose.material.Shapes as Material2Shapes
import androidx.compose.material.Typography as Material2Typography
import androidx.compose.material.darkColors
import androidx.compose.material.Surface as Material2Surface
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.Typography as Material3Typography
import androidx.compose.material3.Shapes as Material3Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * ---------------------------------------------------------------------------
 * Material 2 compatibility theme
 * ---------------------------------------------------------------------------
 *
 * AuralArc is being migrated screen-by-screen. These values remain available
 * so screens that have not yet been converted do not suddenly change style.
 */

private val AuralArcMaterial2Colors =
    darkColors(
        primary =
            AuralArcStyle.Purple,

        primaryVariant =
            AuralArcStyle.PurpleDark,

        secondary =
            AuralArcStyle.PurpleBright,

        background =
            AuralArcStyle.BackgroundBottom,

        surface =
            AuralArcStyle.Surface,

        error =
            AuralArcStyle.Error,

        onPrimary =
            AuralArcStyle.TextPrimary,

        onSecondary =
            AuralArcStyle.BackgroundBottom,

        onBackground =
            AuralArcStyle.TextPrimary,

        onSurface =
            AuralArcStyle.TextPrimary,

        onError =
            AuralArcStyle.TextPrimary
    )

private val AuralArcMaterial2Typography =
    Material2Typography(
        defaultFontFamily =
            FontFamily.SansSerif,

        h4 =
            TextStyle(
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    32.sp,
                letterSpacing =
                    (-0.5).sp
            ),

        h5 =
            TextStyle(
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    26.sp,
                letterSpacing =
                    (-0.25).sp
            ),

        h6 =
            TextStyle(
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    20.sp
            ),

        subtitle1 =
            TextStyle(
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    16.sp
            ),

        body1 =
            TextStyle(
                fontWeight =
                    FontWeight.Normal,
                fontSize =
                    16.sp
            ),

        body2 =
            TextStyle(
                fontWeight =
                    FontWeight.Normal,
                fontSize =
                    14.sp
            ),

        button =
            TextStyle(
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    14.sp,
                letterSpacing =
                    0.25.sp
            ),

        caption =
            TextStyle(
                fontWeight =
                    FontWeight.Medium,
                fontSize =
                    12.sp,
                letterSpacing =
                    0.2.sp
            )
    )

private val AuralArcMaterial2Shapes =
    Material2Shapes(
        small =
            AuralArcStyle.SmallShape,

        medium =
            AuralArcStyle.CardShape,

        large =
            AuralArcStyle.LargeShape
    )

/*
 * ---------------------------------------------------------------------------
 * Material 3 color scheme
 * ---------------------------------------------------------------------------
 */

private val AuralArcMaterial3Colors =
    darkColorScheme(
        primary =
            AuralArcStyle.Purple,

        onPrimary =
            AuralArcStyle.TextPrimary,

        primaryContainer =
            AuralArcStyle.PurpleDark,

        onPrimaryContainer =
            AuralArcStyle.TextPrimary,

        secondary =
            AuralArcStyle.PurpleBright,

        onSecondary =
            AuralArcStyle.BackgroundBottom,

        secondaryContainer =
            AuralArcStyle.SurfaceSoft,

        onSecondaryContainer =
            AuralArcStyle.TextPrimary,

        tertiary =
            AuralArcStyle.PurpleBright,

        onTertiary =
            AuralArcStyle.BackgroundBottom,

        background =
            AuralArcStyle.BackgroundBottom,

        onBackground =
            AuralArcStyle.TextPrimary,

        surface =
            AuralArcStyle.Surface,

        onSurface =
            AuralArcStyle.TextPrimary,

        surfaceVariant =
            AuralArcStyle.SurfaceBright,

        onSurfaceVariant =
            AuralArcStyle.TextSecondary,

        surfaceContainerLowest =
            AuralArcStyle.SurfaceContainerLowest,

        surfaceContainerLow =
            AuralArcStyle.SurfaceContainerLow,

        surfaceContainer =
            AuralArcStyle.SurfaceContainer,

        surfaceContainerHigh =
            AuralArcStyle.SurfaceContainerHigh,

        surfaceContainerHighest =
            AuralArcStyle.SurfaceContainerHighest,

        error =
            AuralArcStyle.Error,

        onError =
            AuralArcStyle.TextPrimary
    )

/*
 * ---------------------------------------------------------------------------
 * Material 3 typography
 * ---------------------------------------------------------------------------
 */

private val AuralArcMaterial3Typography =
    Material3Typography(
        displayLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    32.sp,
                lineHeight =
                    38.sp,
                letterSpacing =
                    (-0.5).sp
            ),

        displayMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    28.sp,
                lineHeight =
                    34.sp
            ),

        headlineLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    26.sp,
                lineHeight =
                    32.sp
            ),

        headlineMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    22.sp,
                lineHeight =
                    28.sp
            ),

        headlineSmall =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    20.sp,
                lineHeight =
                    26.sp
            ),

        titleLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    20.sp,
                lineHeight =
                    26.sp
            ),

        titleMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    16.sp,
                lineHeight =
                    22.sp
            ),

        titleSmall =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    14.sp,
                lineHeight =
                    20.sp
            ),

        bodyLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Normal,
                fontSize =
                    16.sp,
                lineHeight =
                    22.sp
            ),

        bodyMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Normal,
                fontSize =
                    14.sp,
                lineHeight =
                    20.sp
            ),

        bodySmall =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Normal,
                fontSize =
                    12.sp,
                lineHeight =
                    18.sp
            ),

        labelLarge =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    14.sp,
                lineHeight =
                    20.sp,
                letterSpacing =
                    0.25.sp
            ),

        labelMedium =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    12.sp,
                lineHeight =
                    16.sp
            ),

        labelSmall =
            TextStyle(
                fontFamily =
                    FontFamily.SansSerif,
                fontWeight =
                    FontWeight.Medium,
                fontSize =
                    11.sp,
                lineHeight =
                    16.sp
            )
    )

private val AuralArcMaterial3Shapes =
    Material3Shapes(
        extraSmall =
            AuralArcStyle.ExtraSmallShape,

        small =
            AuralArcStyle.SmallShape,

        medium =
            AuralArcStyle.CardShape,

        large =
            AuralArcStyle.LargeShape,

        extraLarge =
            RoundedCornerShape(
                32.dp
            )
    )

@Composable
fun AuralArcTheme(
    content: @Composable () -> Unit
) {
    Material2Theme(
        colors =
            AuralArcMaterial2Colors,

        typography =
            AuralArcMaterial2Typography,

        shapes =
            AuralArcMaterial2Shapes
    ) {
        Material3Theme(
            colorScheme =
                AuralArcMaterial3Colors,

            typography =
                AuralArcMaterial3Typography,

            shapes =
                AuralArcMaterial3Shapes
        ) {
            Material2Surface(
                modifier =
                    Modifier.fillMaxSize(),

                color =
                    AuralArcStyle.BackgroundBottom,

                contentColor =
                    AuralArcStyle.TextPrimary
            ) {
                CompositionLocalProvider(
                    androidx.compose.material.LocalContentColor provides
                            AuralArcStyle.TextPrimary,

                    androidx.compose.material3.LocalContentColor provides
                            AuralArcStyle.TextPrimary
                ) {
                    content()
                }
            }
        }
    }
}