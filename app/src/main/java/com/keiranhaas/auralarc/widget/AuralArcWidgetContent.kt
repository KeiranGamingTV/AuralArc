package com.keiranhaas.auralarc.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.keiranhaas.auralarc.R
import com.keiranhaas.auralarc.player.AuralArcRepeatMode
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

/* ---------------------------- Small (2x1) ---------------------------- */

@Composable
fun SmallWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(8.dp)
            .clickable(WidgetActions.openApp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtwork(
            snapshot = snapshot,
            size = 52.dp
        )

        Spacer(
            modifier = GlanceModifier.defaultWeight()
        )

        PlayPauseButton(
            context = context,
            snapshot = snapshot,
            circleSize = 44.dp,
            iconSize = 26.dp
        )
    }
}

/* ---------------------------- Medium (4x2) --------------------------- */

@Composable
fun MediumWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp)
            .clickable(WidgetActions.openApp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtwork(
            snapshot = snapshot,
            size = 88.dp
        )

        Spacer(
            modifier = GlanceModifier.width(12.dp)
        )

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.titleText(),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(AuralArcStyle.TextPrimary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = snapshot.artistText(),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(AuralArcStyle.TextSecondary),
                    fontSize = 13.sp
                )
            )

            Spacer(
                modifier = GlanceModifier.height(8.dp)
            )

            ControlsRow(
                context = context,
                snapshot = snapshot,
                sideIconSize = 20.dp,
                skipIconSize = 22.dp,
                playCircleSize = 34.dp,
                playIconSize = 20.dp
            )
        }
    }
}

/* ----------------------------- Large (4x4) --------------------------- */

@Composable
fun LargeWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(16.dp)
            .clickable(WidgetActions.openApp()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WidgetArtwork(
            snapshot = snapshot,
            size = 190.dp
        )

        Spacer(
            modifier = GlanceModifier.height(14.dp)
        )

        Text(
            text = snapshot.titleText(),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = ColorProvider(AuralArcStyle.TextPrimary),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Text(
            text = snapshot.artistText(),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = ColorProvider(AuralArcStyle.TextSecondary),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(
            modifier = GlanceModifier.defaultWeight()
        )

        ControlsRow(
            context = context,
            snapshot = snapshot,
            sideIconSize = 26.dp,
            skipIconSize = 34.dp,
            playCircleSize = 56.dp,
            playIconSize = 32.dp
        )
    }
}

/* ---------------------------- Shared pieces -------------------------- */

private fun WidgetSnapshot.titleText(): String =
    if (hasTrack) title else "Nothing playing"

private fun WidgetSnapshot.artistText(): String =
    if (hasTrack) {
        artist.ifBlank { "Unknown Artist" }
    } else {
        "Tap to open AuralArc"
    }

@Composable
private fun WidgetArtwork(
    snapshot: WidgetSnapshot,
    size: Dp
) {
    val artwork =
        snapshot.artwork

    if (artwork != null) {
        Image(
            provider = ImageProvider(artwork),
            contentDescription = "Album artwork",
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(size)
        )
    } else {
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(ImageProvider(R.drawable.widget_art_placeholder)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_music_note),
                contentDescription = null,
                modifier = GlanceModifier.size(size / 2),
                colorFilter = ColorFilter.tint(
                    ColorProvider(AuralArcStyle.PurpleBright)
                )
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    context: Context,
    snapshot: WidgetSnapshot,
    circleSize: Dp,
    iconSize: Dp
) {
    Box(
        modifier = GlanceModifier
            .size(circleSize)
            .background(ImageProvider(R.drawable.widget_play_circle))
            .clickable(WidgetActions.playPause(context)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(
                if (snapshot.isPlaying) {
                    R.drawable.ic_widget_pause
                } else {
                    R.drawable.ic_widget_play
                }
            ),
            contentDescription =
                if (snapshot.isPlaying) "Pause" else "Play",
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(
                ColorProvider(AuralArcStyle.TextPrimary)
            )
        )
    }
}

@Composable
private fun ControlsRow(
    context: Context,
    snapshot: WidgetSnapshot,
    sideIconSize: Dp,
    skipIconSize: Dp,
    playCircleSize: Dp,
    playIconSize: Dp
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlSlot {
            ControlIcon(
                iconRes = R.drawable.ic_notification_shuffle,
                description =
                    if (snapshot.shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint =
                    if (snapshot.shuffleEnabled) {
                        AuralArcStyle.PurpleBright
                    } else {
                        AuralArcStyle.TextMuted
                    },
                iconSize = sideIconSize,
                action = WidgetActions.shuffle(context)
            )
        }

        ControlSlot {
            ControlIcon(
                iconRes = R.drawable.ic_widget_skip_previous,
                description = "Previous",
                tint = AuralArcStyle.TextPrimary,
                iconSize = skipIconSize,
                action = WidgetActions.previous(context)
            )
        }

        ControlSlot {
            PlayPauseButton(
                context = context,
                snapshot = snapshot,
                circleSize = playCircleSize,
                iconSize = playIconSize
            )
        }

        ControlSlot {
            ControlIcon(
                iconRes = R.drawable.ic_widget_skip_next,
                description = "Next",
                tint = AuralArcStyle.TextPrimary,
                iconSize = skipIconSize,
                action = WidgetActions.next(context)
            )
        }

        ControlSlot {
            ControlIcon(
                iconRes =
                    if (snapshot.repeatMode == AuralArcRepeatMode.ONE) {
                        R.drawable.ic_notification_repeat_one
                    } else {
                        R.drawable.ic_notification_repeat
                    },
                description =
                    when (snapshot.repeatMode) {
                        AuralArcRepeatMode.OFF -> "Repeat off"
                        AuralArcRepeatMode.ALL -> "Repeat all"
                        AuralArcRepeatMode.ONE -> "Repeat one"
                    },
                tint =
                    if (snapshot.repeatMode == AuralArcRepeatMode.OFF) {
                        AuralArcStyle.TextMuted
                    } else {
                        AuralArcStyle.PurpleBright
                    },
                iconSize = sideIconSize,
                action = WidgetActions.repeat(context)
            )
        }
    }
}

/**
 * Gives each control an equal share of the row so the buttons stay
 * evenly spaced at any widget width.
 */
@Composable
private fun RowScope.ControlSlot(
    content: @Composable () -> Unit
) {
    Box(
        modifier = GlanceModifier.defaultWeight(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ControlIcon(
    iconRes: Int,
    description: String,
    tint: Color,
    iconSize: Dp,
    action: Action
) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = description,
        modifier = GlanceModifier
            .size(iconSize)
            .clickable(action),
        colorFilter = ColorFilter.tint(
            ColorProvider(tint)
        )
    )
}