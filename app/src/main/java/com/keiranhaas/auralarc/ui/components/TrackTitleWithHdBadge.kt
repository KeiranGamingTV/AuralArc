package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun TrackTitleWithHdBadge(
    track: MusicTrack,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body1,
    color: Color = AuralArcStyle.TextPrimary,
    fontWeight: FontWeight? = FontWeight.Bold,
    maxLines: Int = 1,
    horizontalArrangement: Arrangement.Horizontal =
        Arrangement.Start,
    fillTitleWeight: Boolean = false,
    marqueeWhenOverflow: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    centerTitleIndependently: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        /*
        * On centered layouts such as Now Playing, an HD badge
        * on only the right side reduces the title's available
        * width from that side and shifts the title's true center
        * slightly left.
        *
        * Add an invisible badge of the exact same size on the
        * left so the title receives perfectly symmetrical space.
        */
        if (
            centerTitleIndependently &&
            track.isHighResolution
        ) {
            AuralArcHdBadge(
                modifier = Modifier
                    .alpha(
                        0f
                    )
                    .clearAndSetSemantics {
                    }
            )

            Spacer(
                modifier = Modifier.width(
                    7.dp
                )
            )
        }

        val titleModifier =
            if (
                fillTitleWeight ||
                track.isHighResolution
            ) {
                Modifier.weight(
                    1f
                )
            } else {
                Modifier
            }

        if (
            marqueeWhenOverflow &&
            maxLines == 1
        ) {
            OscillatingMarqueeText(
                text = track.title,
                modifier = titleModifier,
                style = style,
                color = color,
                fontWeight = fontWeight,
                textAlign = textAlign
            )
        } else {
            Text(
                text = track.title,
                modifier = titleModifier,
                style = style,
                fontWeight = fontWeight,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign
            )
        }

        if (
            track.isHighResolution
        ) {
            Spacer(
                modifier = Modifier.width(
                    7.dp
                )
            )

            AuralArcHdBadge()
        }
    }
}

@Composable
fun AuralArcHdBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(
                18.dp
            )
            .background(
                color = AuralArcStyle.PurpleBright,
                shape = RoundedCornerShape(
                    50
                )
            )
            .padding(
                horizontal = 7.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "HD",
            style = MaterialTheme.typography.overline,
            fontWeight = FontWeight.Bold,
            color = AuralArcStyle.TextPrimary,
            maxLines = 1
        )
    }
}