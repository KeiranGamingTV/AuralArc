package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

/**
 * One tile of the Albums / Artists / Playlists grid view.
 * [art] receives the tile's width so it can draw a square image.
 */
@Composable
internal fun LibraryGridTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable (Dp) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .auralArcClickable {
                onClick()
            }
            .padding(
                vertical = 4.dp
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            art(
                maxWidth
            )
        }

        Spacer(
            modifier = Modifier.height(
                8.dp
            )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.SemiBold,
            color = AuralArcStyle.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.caption,
            color = AuralArcStyle.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}