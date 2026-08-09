package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun PlaceholderArtwork(
    size: Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .size(
                size
            )
            .background(
                brush = AuralArcStyle.artworkBrush(),
                shape = AuralArcStyle.ArtworkShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "OA",
            color = AuralArcStyle.TextPrimary,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.ExtraBold
        )
    }
}