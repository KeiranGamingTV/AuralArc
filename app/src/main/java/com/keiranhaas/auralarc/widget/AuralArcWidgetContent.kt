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
import androidx.glance.LocalSize
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

@Composable
fun SmallWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    AdaptiveWidgetContent(
        context = context,
        snapshot = snapshot
    )
}

@Composable
fun MediumWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    AdaptiveWidgetContent(
        context = context,
        snapshot = snapshot
    )
}

@Composable
fun LargeWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    AdaptiveWidgetContent(
        context = context,
        snapshot = snapshot
    )
}

/* ------------------------ Size-based layouts ------------------------- */

private const val COLUMN_PADDING = 14f

@Composable
private fun AdaptiveWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot
) {
    val size = LocalSize.current
    val width = size.width.value
    val height = size.height.value

    when {
        height < 90f && width < 200f ->
            CompactLayout(context, snapshot, height)

        height < 90f ->
            BannerLayout(context, snapshot, height)

        width < height * 1.45f &&
                columnArtSize(width, height) >= 64f ->
            ColumnLayout(context, snapshot, width, height)

        else ->
            RowLayout(context, snapshot, width, height)
    }
}

@Composable
private fun CompactLayout(
    context: Context,
    snapshot: WidgetSnapshot,
    height: Float
) {
    val padding = 8f
    val art = (height - padding * 2).coerceIn(32f, 72f)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(padding.dp)
            .clickable(WidgetActions.openApp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtwork(
            snapshot = snapshot,
            size = art.dp
        )

        Spacer(
            modifier = GlanceModifier.defaultWeight()
        )

        PlayPauseButton(
            context = context,
            snapshot = snapshot,
            circleSize = (art * 0.85f).dp,
            iconSize = (art * 0.5f).dp
        )
    }
}

@Composable
private fun BannerLayout(
    context: Context,
    snapshot: WidgetSnapshot,
    height: Float
) {
    val padding = 8f
    val art = (height - padding * 2).coerceIn(32f, 72f)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(padding.dp)
            .clickable(WidgetActions.openApp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtwork(
            snapshot = snapshot,
            size = art.dp
        )

        Spacer(
            modifier = GlanceModifier.width(10.dp)
        )

        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snapshot.titleText(),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(AuralArcStyle.TextPrimary),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = snapshot.artistText(),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(AuralArcStyle.TextSecondary),
                    fontSize = 12.sp
                )
            )
        }

        Spacer(
            modifier = GlanceModifier.width(10.dp)
        )

        PlayPauseButton(
            context = context,
            snapshot = snapshot,
            circleSize = (art * 0.85f).dp,
            iconSize = (art * 0.5f).dp
        )
    }
}

@Composable
private fun RowLayout(
    context: Context,
    snapshot: WidgetSnapshot,
    width: Float,
    height: Float
) {
    val padding = 12f
    val gap = 12f

    val art =
        minOf(height - padding * 2, width * 0.42f)
            .coerceIn(48f, 240f)

    val textWidth = width - padding * 2 - art - gap
    val titleSp = (height * 0.13f).coerceIn(14f, 22f)
    val artistSp = (titleSp - 3f).coerceAtLeast(11f)
    val controlsGap = (height * 0.06f).coerceIn(6f, 16f)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(padding.dp)
            .clickable(WidgetActions.openApp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WidgetArtwork(
            snapshot = snapshot,
            size = art.dp
        )

        Spacer(
            modifier = GlanceModifier.width(gap.dp)
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
                    fontSize = titleSp.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = snapshot.artistText(),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(AuralArcStyle.TextSecondary),
                    fontSize = artistSp.sp
                )
            )

            Spacer(
                modifier = GlanceModifier.height(controlsGap.dp)
            )

            ControlsRow(
                context = context,
                snapshot = snapshot,
                rowWidth = textWidth
            )
        }
    }
}

@Composable
private fun ColumnLayout(
    context: Context,
    snapshot: WidgetSnapshot,
    width: Float,
    height: Float
) {
    val innerWidth = width - COLUMN_PADDING * 2
    val art = columnArtSize(width, height)
    val titleSp = columnTitleSp(width)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(COLUMN_PADDING.dp)
            .clickable(WidgetActions.openApp()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            val artwork =
                snapshot.artwork

            if (artwork != null) {
                Image(
                    provider = ImageProvider(artwork),
                    contentDescription = "Album artwork",
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxSize()
                )
            } else {
                WidgetArtwork(
                    snapshot = snapshot,
                    size = art.dp
                )
            }
        }

        Spacer(
            modifier = GlanceModifier.height(12.dp)
        )

        Text(
            text = snapshot.titleText(),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = ColorProvider(AuralArcStyle.TextPrimary),
                fontSize = titleSp.sp,
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
                fontSize = (titleSp * 0.75f).sp,
                textAlign = TextAlign.Center
            )
        )

        Spacer(
            modifier = GlanceModifier.height(16.dp)
        )

        ControlsRow(
            context = context,
            snapshot = snapshot,
            rowWidth = innerWidth
        )
    }
}

private fun columnTitleSp(
    width: Float
): Float {
    return ((width - COLUMN_PADDING * 2) * 0.08f)
        .coerceIn(15f, 24f)
}

private fun columnArtSize(
    width: Float,
    height: Float
): Float {
    val innerWidth = width - COLUMN_PADDING * 2
    val innerHeight = height - COLUMN_PADDING * 2
    val titleSp = columnTitleSp(width)
    val textHeight = (titleSp + titleSp * 0.75f) * 1.3f
    val reserved = textHeight + 28f + controlCircle(innerWidth)

    return minOf(innerWidth, innerHeight - reserved)
        .coerceAtMost(320f)
}

private fun controlSlotCount(
    rowWidth: Float
): Int {
    return when {
        rowWidth >= 150f -> 5
        rowWidth >= 100f -> 3
        else -> 1
    }
}

private fun controlCircle(
    rowWidth: Float
): Float {
    return (rowWidth / controlSlotCount(rowWidth) * 0.85f)
        .coerceIn(34f, 64f)
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
    rowWidth: Float
) {
    val slots = controlSlotCount(rowWidth)
    val slotWidth = rowWidth / slots
    val circle = controlCircle(rowWidth)
    val sideIconSize = (slotWidth * 0.38f).coerceIn(20f, 28f).dp
    val skipIconSize = (slotWidth * 0.5f).coerceIn(22f, 36f).dp

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (slots == 5) {
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
        }

        if (slots >= 3) {
            ControlSlot {
                ControlIcon(
                    iconRes = R.drawable.ic_widget_skip_previous,
                    description = "Previous",
                    tint = AuralArcStyle.TextPrimary,
                    iconSize = skipIconSize,
                    action = WidgetActions.previous(context)
                )
            }
        }

        ControlSlot {
            PlayPauseButton(
                context = context,
                snapshot = snapshot,
                circleSize = circle.dp,
                iconSize = (circle * 0.58f).dp
            )
        }

        if (slots >= 3) {
            ControlSlot {
                ControlIcon(
                    iconRes = R.drawable.ic_widget_skip_next,
                    description = "Next",
                    tint = AuralArcStyle.TextPrimary,
                    iconSize = skipIconSize,
                    action = WidgetActions.next(context)
                )
            }
        }

        if (slots == 5) {
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
}

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