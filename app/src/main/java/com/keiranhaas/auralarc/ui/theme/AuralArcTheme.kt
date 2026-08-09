package com.keiranhaas.auralarc.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AuralArcColors =
    darkColors(
        primary = AuralArcStyle.Purple,
        primaryVariant = AuralArcStyle.PurpleDark,
        secondary = AuralArcStyle.PurpleBright,
        background = AuralArcStyle.BackgroundBottom,
        surface = AuralArcStyle.Surface,
        error = androidx.compose.ui.graphics.Color(0xFFFF6B8A),
        onPrimary = AuralArcStyle.TextPrimary,
        onSecondary = AuralArcStyle.BackgroundBottom,
        onBackground = AuralArcStyle.TextPrimary,
        onSurface = AuralArcStyle.TextPrimary,
        onError = AuralArcStyle.TextPrimary
    )

private val AuralArcTypography =
    Typography(
        defaultFontFamily = FontFamily.SansSerif,

        h4 = TextStyle(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            letterSpacing = (-0.5).sp
        ),

        h5 = TextStyle(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            letterSpacing = (-0.25).sp
        ),

        h6 = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 0.sp
        ),

        subtitle1 = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.sp
        ),

        body1 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.sp
        ),

        body2 = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 0.sp
        ),

        button = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.25.sp
        ),

        caption = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.2.sp
        )
    )

private val AuralArcShapes =
    Shapes(
        small = AuralArcStyle.SmallShape,
        medium = AuralArcStyle.CardShape,
        large = androidx.compose.foundation.shape.RoundedCornerShape(
            28.dp
        )
    )

@Composable
fun AuralArcTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = AuralArcColors,
        typography = AuralArcTypography,
        shapes = AuralArcShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
            contentColor = AuralArcStyle.TextPrimary
        ) {
            CompositionLocalProvider(
                LocalContentColor provides AuralArcStyle.TextPrimary
            ) {
                content()
            }
        }
    }
}