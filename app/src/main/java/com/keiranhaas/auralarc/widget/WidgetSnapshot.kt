package com.keiranhaas.auralarc.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.keiranhaas.auralarc.player.AuralArcRepeatMode
import com.keiranhaas.auralarc.player.PlaybackState
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.utils.ArtworkBitmapLoader
import kotlin.math.max

/**
 * Everything a widget needs to draw itself, captured once per update.
 */
data class WidgetSnapshot(
    val hasTrack: Boolean,
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val isPlaying: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: AuralArcRepeatMode
) {

    companion object {

        suspend fun load(
            context: Context,
            artworkDp: Int
        ): WidgetSnapshot {
            val appContext =
                context.applicationContext

            if (
                PlayerManager.currentTitle.value.isBlank()
            ) {
                PlayerManager.restoreSavedDisplayState(
                    appContext
                )
            }

            val title =
                PlayerManager.currentTitle.value

            val artworkPath =
                PlayerManager.currentAlbumArtPath.value.trim()

            val density =
                appContext.resources.displayMetrics.density

            val sizePx =
                (artworkDp * density)
                    .toInt()
                    .coerceIn(
                        64,
                        768
                    )

            val artwork =
                if (
                    artworkPath.isBlank()
                ) {
                    null
                } else {
                    ArtworkBitmapLoader.load(
                        context = appContext,
                        artworkPath = artworkPath,
                        targetSizePx = sizePx
                    )?.let { loaded ->
                        roundedSquare(
                            source = loaded,
                            sizePx = sizePx,
                            radiusPx = sizePx * 0.12f
                        )
                    }
                }

            return WidgetSnapshot(
                hasTrack = title.isNotBlank(),
                title = title,
                artist = PlayerManager.currentArtist.value,
                artwork = artwork,
                isPlaying = PlayerManager.isPlaying.value,
                shuffleEnabled = PlaybackState.shuffleEnabled.value,
                repeatMode = PlaybackState.repeatMode.value
            )
        }

        private fun roundedSquare(
            source: Bitmap,
            sizePx: Int,
            radiusPx: Float
        ): Bitmap {
            val output =
                Bitmap.createBitmap(
                    sizePx,
                    sizePx,
                    Bitmap.Config.ARGB_8888
                )

            val shader =
                BitmapShader(
                    source,
                    Shader.TileMode.CLAMP,
                    Shader.TileMode.CLAMP
                )

            val scale =
                max(
                    sizePx / source.width.toFloat(),
                    sizePx / source.height.toFloat()
                )

            shader.setLocalMatrix(
                Matrix().apply {
                    setScale(
                        scale,
                        scale
                    )

                    postTranslate(
                        (sizePx - source.width * scale) / 2f,
                        (sizePx - source.height * scale) / 2f
                    )
                }
            )

            val paint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    this.shader =
                        shader
                }

            Canvas(
                output
            ).drawRoundRect(
                RectF(
                    0f,
                    0f,
                    sizePx.toFloat(),
                    sizePx.toFloat()
                ),
                radiusPx,
                radiusPx,
                paint
            )

            return output
        }
    }
}