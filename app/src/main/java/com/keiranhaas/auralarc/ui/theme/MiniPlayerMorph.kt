package com.keiranhaas.auralarc.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.navigation.Screen
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.ui.TrackArtwork
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlinx.coroutines.cancelAndJoin

/*
 * Mini player <-> Now Playing morph.
 *
 * Both screens register where their album art, title and artist are
 * ("slots"). A transparent overlay above the whole NavHost draws a copy
 * of those three elements and animates it between the two layouts, while
 * the real elements are hidden. Because the overlay lives outside the
 * screens, it keeps moving while the screens fade underneath it.
 */

enum class MorphSlot {
    MiniArt,
    MiniTitle,
    MiniArtist,
    NowPlayingArt,
    NowPlayingTitle,
    NowPlayingArtist
}

internal class MorphRects(
    val art: Rect,
    val title: Rect,
    val artist: Rect
)

object MiniPlayerMorphState {

    val slots =
        mutableStateMapOf<MorphSlot, Rect>()

    /*
     * True between tapping the mini player and Now Playing finishing
     * its entrance. Navigation reads it to choose a fade instead of
     * the slide-up.
     */
    @Volatile
    var armed: Boolean =
        false

    /* True while the overlay is flying. Real elements hide meanwhile. */
    var active by mutableStateOf(false)
        internal set

    /* Layouts the overlay flies between (frozen at flight start). */
    internal var nowPlayingRects by mutableStateOf<MorphRects?>(null)

    internal var miniRects by mutableStateOf<MorphRects?>(null)

    /* 0 = at the mini player, 1 = at Now Playing. */
    internal val progress =
        Animatable(0f)

    fun armFromMiniPlayer(
        motionEnabled: Boolean
    ) {
        armed =
            motionEnabled &&
                    slots.containsKey(MorphSlot.MiniArt)
    }

    private fun readNowPlaying(): MorphRects? {
        val art = slots[MorphSlot.NowPlayingArt]
        val title = slots[MorphSlot.NowPlayingTitle]
        val artist = slots[MorphSlot.NowPlayingArtist]

        if (
            art == null ||
            title == null ||
            artist == null ||
            art.top < 0f
        ) {
            return null
        }

        return MorphRects(art, title, artist)
    }

    private fun readMini(): MorphRects? {
        val art = slots[MorphSlot.MiniArt]
        val title = slots[MorphSlot.MiniTitle]
        val artist = slots[MorphSlot.MiniArtist]

        if (
            art == null ||
            title == null ||
            artist == null
        ) {
            return null
        }

        return MorphRects(art, title, artist)
    }

    private suspend fun <T : Any> awaitValue(
        timeoutMs: Long,
        read: () -> T?
    ): T? =
        withTimeoutOrNull(timeoutMs) {
            snapshotFlow { read() }.first { it != null }
        }

    internal suspend fun runForward() {
        try {
            val mini =
                readMini()

            val nowPlaying =
                awaitValue(500L) { readNowPlaying() }

            if (
                mini == null ||
                nowPlaying == null
            ) {
                return
            }

            miniRects =
                mini

            nowPlayingRects =
                nowPlaying

            progress.snapTo(0f)

            active =
                true

            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = 380f
                )
            )
        } finally {
            active =
                false

            armed =
                false

            miniRects =
                null

            nowPlayingRects =
                null
        }
    }

    internal suspend fun runReturn() {
        try {
            val nowPlaying =
                readNowPlaying()
                    ?: return

            nowPlayingRects =
                nowPlaying

            miniRects =
                null

            progress.snapTo(1f)

            active =
                true

            val mini =
                awaitValue(1500L) { readMini() }
                    ?: return

            miniRects =
                mini

            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 380f
                )
            )
        } finally {
            active =
                false

            miniRects =
                null

            nowPlayingRects =
                null
        }
    }
}

/**
 * Marks an element as one end of the morph and hides it while the
 * overlay is flying.
 */
@Composable
fun Modifier.morphSlot(
    slot: MorphSlot
): Modifier {
    DisposableEffect(
        slot
    ) {
        onDispose {
            MiniPlayerMorphState.slots.remove(
                slot
            )
        }
    }

    return this
        .onGloballyPositioned { coordinates ->
            MiniPlayerMorphState.slots[slot] =
                coordinates.boundsInRoot()
        }
        .graphicsLayer {
            alpha =
                if (
                    MiniPlayerMorphState.active
                ) {
                    0f
                } else {
                    1f
                }
        }
}

/**
 * Place once, above the NavHost, inside the same full-screen Box.
 */
@Composable
fun MiniPlayerMorphOverlay(
    navController: NavHostController
) {
    val motionEnabled =
        rememberAuralArcMotionEnabled()

    LaunchedEffect(
        navController,
        motionEnabled
    ) {
        var previousRoute =
            navController.currentDestination?.route

        var flightJob: Job? =
            null

        navController.currentBackStackEntryFlow.collect { entry ->
            val route =
                entry.destination.route

            val previous =
                previousRoute

            previousRoute =
                route

            if (
                !motionEnabled
            ) {
                return@collect
            }

            val isReturn =
                previous == Screen.NowPlaying.route &&
                        route == Screen.Library.route

            val isForward =
                route == Screen.NowPlaying.route &&
                        MiniPlayerMorphState.armed

            if (
                !isReturn &&
                !isForward
            ) {
                return@collect
            }

            val earlier =
                flightJob

            flightJob =
                launch {
                    earlier?.cancelAndJoin()

                    if (
                        isReturn
                    ) {
                        MiniPlayerMorphState.runReturn()
                    } else {
                        MiniPlayerMorphState.runForward()
                    }
                }
        }
    }

    val density =
        LocalDensity.current

    val typography =
        MaterialTheme.typography

    /*
     * Mini player text is smaller than Now Playing text; scale the
     * copies by the real font-size ratio so the sizes match at both ends.
     */
    val titleRatio =
        typography.bodyLarge.fontSize.value /
                typography.headlineLarge.fontSize.value

    val artistRatio =
        typography.bodyMedium.fontSize.value /
                typography.titleMedium.fontSize.value

    var origin by remember {
        mutableStateOf(
            Offset.Zero
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                origin =
                    coordinates.positionInRoot()
            }
    ) {
        val from =
            MiniPlayerMorphState.nowPlayingRects

        val to =
            MiniPlayerMorphState.miniRects

        if (
            MiniPlayerMorphState.active &&
            from != null
        ) {
            MorphElement(
                fromRect = from.art,
                toRect = to?.art,
                fixedRatio = null,
                origin = origin
            ) {
                TrackArtwork(
                    albumArtPath =
                        PlayerManager.currentAlbumArtPath.value,
                    size = with(density) {
                        from.art.width.toDp()
                    }
                )
            }

            MorphElement(
                fromRect = from.title,
                toRect = to?.title,
                fixedRatio = titleRatio,
                origin = origin
            ) {
                Text(
                    text = PlayerManager.currentTitle.value,
                    style = typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }

            MorphElement(
                fromRect = from.artist,
                toRect = to?.artist,
                fixedRatio = artistRatio,
                origin = origin
            ) {
                Text(
                    text = PlayerManager.currentArtist.value.ifBlank {
                        "Unknown Artist"
                    },
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = AuralArcStyle.TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun MorphElement(
    fromRect: Rect,
    toRect: Rect?,
    fixedRatio: Float?,
    origin: Offset,
    content: @Composable () -> Unit
) {
    val density =
        LocalDensity.current

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (fromRect.left - origin.x).roundToInt(),
                    (fromRect.top - origin.y).roundToInt()
                )
            }
            .size(
                width = with(density) {
                    fromRect.width.toDp()
                },
                height = with(density) {
                    fromRect.height.toDp()
                }
            )
            .graphicsLayer {
                val target =
                    toRect
                        ?: return@graphicsLayer

                val amount =
                    MiniPlayerMorphState.progress.value

                transformOrigin =
                    TransformOrigin(
                        0f,
                        0f
                    )

                val ratio =
                    fixedRatio
                        ?: (target.width / fromRect.width)

                val scale =
                    lerp(
                        ratio,
                        1f,
                        amount
                    )

                scaleX =
                    scale

                scaleY =
                    scale

                /*
                 * Left edges line up; vertical centers line up so the
                 * text lands on the real text's baseline.
                 */
                val startX =
                    target.left - fromRect.left

                val startY =
                    target.center.y -
                            fromRect.height * ratio / 2f -
                            fromRect.top

                translationX =
                    startX * (1f - amount)

                translationY =
                    startY * (1f - amount)
            }
    ) {
        content()
    }
}