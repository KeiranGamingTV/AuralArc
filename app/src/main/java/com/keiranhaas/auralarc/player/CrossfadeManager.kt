package com.keiranhaas.auralarc.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.keiranhaas.auralarc.storage.CrossfadePreferences
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object CrossfadeManager {

    private const val TICK_IDLE_MS =
        100L

    private const val TICK_FADING_MS =
        40L

    private const val PREPARE_LEAD_MS =
        3_000L

    var mainMultiplier =
        1f
        private set

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var appContext: Context? =
        null

    private var running =
        false

    private var ghost: ExoPlayer? =
        null

    private var ghostIndex =
        -1

    private var overlapActive =
        false

    private var overlapStartedAt =
        0L

    private var selfTransition =
        false

    private var fadeInActive =
        false

    private var fadeOutActive =
        false

    private val tick =
        object : Runnable {
            override fun run() {
                if (
                    !running
                ) {
                    return
                }

                try {
                    step()
                } catch (_: Exception) {
                }

                if (
                    running
                ) {
                    handler.postDelayed(
                        this,
                        if (
                            overlapActive ||
                            fadeInActive ||
                            fadeOutActive
                        ) {
                            TICK_FADING_MS
                        } else {
                            TICK_IDLE_MS
                        }
                    )
                }
            }
        }

    fun sync(
        context: Context
    ) {
        val app =
            context.applicationContext

        appContext =
            app

        val enabled =
            CrossfadePreferences.getEnabled(
                app
            )

        val wantsToPlay =
            PlayerManager.getPlayer(
                app
            ).playWhenReady

        handler.removeCallbacks(
            tick
        )

        if (
            enabled &&
            wantsToPlay
        ) {
            running =
                true

            handler.post(
                tick
            )
        } else {
            running =
                false

            if (
                !enabled
            ) {
                cancelAll()
            } else {
                releaseGhost()
            }
        }
    }

    fun onMediaItemTransition(
        reason: Int
    ) {
        if (
            selfTransition
        ) {
            selfTransition =
                false

            return
        }

        if (
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
            fadeOutActive
        ) {
            fadeOutActive =
                false

            fadeInActive =
                true

            setMultiplier(
                0f
            )

            return
        }

        cancelAll()
    }

    fun cancelAll() {
        releaseGhost()

        fadeInActive =
            false

        fadeOutActive =
            false

        selfTransition =
            false

        setMultiplier(
            1f
        )
    }

    private fun step() {
        val app =
            appContext
                ?: return

        if (
            !CrossfadePreferences.getEnabled(
                app
            )
        ) {
            cancelAll()

            running =
                false

            return
        }

        val player =
            PlayerManager.getPlayer(
                app
            )

        if (
            !player.playWhenReady
        ) {
            releaseGhost()

            return
        }

        val fadeMs =
            CrossfadePreferences.getSeconds(
                app
            ) * 1000L

        val overlap =
            CrossfadePreferences.getMode(
                app
            ) == CrossfadePreferences.MODE_OVERLAP

        val duration =
            player.duration

        val position =
            player.currentPosition

        val index =
            player.currentMediaItemIndex

        val hasDuration =
            duration != C.TIME_UNSET &&
                    duration > 0L

        val remaining =
            if (
                hasDuration
            ) {
                duration - position
            } else {
                Long.MAX_VALUE
            }

        val canFade =
            hasDuration &&
                    duration > fadeMs * 3L + 2_000L &&
                    player.hasNextMediaItem() &&
                    player.repeatMode != Player.REPEAT_MODE_ONE

        var multiplier =
            1f

        if (
            fadeInActive
        ) {
            if (
                position >= fadeMs
            ) {
                fadeInActive =
                    false
            } else {
                multiplier =
                    sin(
                        position / fadeMs.toFloat() * PI / 2.0
                    ).toFloat()
            }
        }

        fadeOutActive =
            false

        if (
            canFade &&
            !overlap &&
            remaining <= fadeMs
        ) {
            fadeOutActive =
                true

            val progress =
                1f - remaining / fadeMs.toFloat()

            multiplier =
                min(
                    multiplier,
                    cos(
                        progress * PI / 2.0
                    ).toFloat()
                )
        }

        if (
            canFade &&
            overlap &&
            !overlapActive
        ) {
            stepOverlap(
                app = app,
                player = player,
                index = index,
                position = position,
                remaining = remaining,
                duration = duration,
                fadeMs = fadeMs
            )
        }

        stepGhostVolume(
            fadeMs
        )

        setMultiplier(
            multiplier
        )
    }

    private fun stepOverlap(
        app: Context,
        player: ExoPlayer,
        index: Int,
        position: Long,
        remaining: Long,
        duration: Long,
        fadeMs: Long
    ) {
        if (
            ghost != null &&
            ghostIndex != index
        ) {
            releaseGhost()
        }

        if (
            ghost != null &&
            remaining > fadeMs + PREPARE_LEAD_MS + 3_000L
        ) {
            releaseGhost()
        }

        if (
            ghost == null &&
            remaining <= fadeMs + PREPARE_LEAD_MS
        ) {
            prepareGhost(
                app = app,
                player = player,
                index = index,
                duration = duration,
                fadeMs = fadeMs
            )
        }

        val activeGhost =
            ghost
                ?: return

        if (
            remaining <= fadeMs &&
            activeGhost.playbackState == Player.STATE_READY
        ) {
            activeGhost.seekTo(
                position
            )

            activeGhost.volume =
                PlayerManager.currentBaseVolume()

            activeGhost.play()

            overlapActive =
                true

            overlapStartedAt =
                SystemClock.elapsedRealtime()

            selfTransition =
                true

            fadeInActive =
                true

            player.seekToNextMediaItem()

            setMultiplier(
                0f
            )
        }
    }

    private fun prepareGhost(
        app: Context,
        player: ExoPlayer,
        index: Int,
        duration: Long,
        fadeMs: Long
    ) {
        val item =
            player.currentMediaItem
                ?: return

        val newGhost =
            ExoPlayer.Builder(
                app
            ).build()

        newGhost.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MUSIC
                )
                .build(),
            false
        )

        newGhost.volume =
            0f

        newGhost.setMediaItem(
            item
        )

        newGhost.prepare()

        newGhost.seekTo(
            (duration - fadeMs).coerceAtLeast(
                0L
            )
        )

        newGhost.playWhenReady =
            false

        ghost =
            newGhost

        ghostIndex =
            index
    }

    private fun stepGhostVolume(
        fadeMs: Long
    ) {
        val activeGhost =
            ghost
                ?: return

        if (
            !overlapActive
        ) {
            return
        }

        val progress =
            (
                    SystemClock.elapsedRealtime() -
                            overlapStartedAt
                    ) / fadeMs.toFloat()

        if (
            progress >= 1f ||
            activeGhost.playbackState == Player.STATE_ENDED
        ) {
            releaseGhost()

            return
        }

        activeGhost.volume =
            (
                    PlayerManager.currentBaseVolume() *
                            cos(
                                progress * PI / 2.0
                            ).toFloat()
                    ).coerceIn(
                    0f,
                    1f
                )
    }

    private fun releaseGhost() {
        try {
            ghost?.release()
        } catch (_: Exception) {
        }

        ghost =
            null

        ghostIndex =
            -1

        overlapActive =
            false
    }

    private fun setMultiplier(
        value: Float
    ) {
        val safeValue =
            value.coerceIn(
                0f,
                1f
            )

        if (
            abs(
                safeValue - mainMultiplier
            ) < 0.004f &&
            !(
                    safeValue == 1f &&
                            mainMultiplier != 1f
                    )
        ) {
            return
        }

        mainMultiplier =
            safeValue

        PlayerManager.refreshEffectivePlayerVolume()
    }
}