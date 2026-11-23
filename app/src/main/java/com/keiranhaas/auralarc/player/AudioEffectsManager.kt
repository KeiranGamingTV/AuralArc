package com.keiranhaas.auralarc.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.compose.runtime.mutableStateOf
import com.keiranhaas.auralarc.storage.AudioEffectsPreferences
import kotlin.math.roundToInt

object AudioEffectsManager {

    private var bassBoost: BassBoost? =
        null

    private var virtualizer: Virtualizer? =
        null

    private var loudnessEnhancer: LoudnessEnhancer? =
        null

    private var attachedSessionId =
        0

    val bassSupported =
        mutableStateOf(
            false
        )

    val virtualizerSupported =
        mutableStateOf(
            false
        )

    val bassEnabled =
        mutableStateOf(
            false
        )

    val bassStrength =
        mutableStateOf(
            500
        )

    val virtualizerEnabled =
        mutableStateOf(
            false
        )

    val virtualizerStrength =
        mutableStateOf(
            500
        )

    val loudnessBoostDb =
        mutableStateOf(
            0f
        )

    fun attach(
        context: Context,
        audioSessionId: Int
    ) {
        if (
            audioSessionId == 0
        ) {
            return
        }

        if (
            attachedSessionId == audioSessionId
        ) {
            return
        }

        release()

        attachedSessionId =
            audioSessionId

        val app =
            context.applicationContext

        bassEnabled.value =
            AudioEffectsPreferences.getBassEnabled(
                app
            )

        bassStrength.value =
            AudioEffectsPreferences.getBassStrength(
                app
            )

        virtualizerEnabled.value =
            AudioEffectsPreferences.getVirtualizerEnabled(
                app
            )

        virtualizerStrength.value =
            AudioEffectsPreferences.getVirtualizerStrength(
                app
            )

        loudnessBoostDb.value =
            AudioEffectsPreferences.getLoudnessBoostDb(
                app
            )

        bassBoost =
            try {
                BassBoost(
                    0,
                    audioSessionId
                )
            } catch (_: Exception) {
                null
            }

        virtualizer =
            try {
                Virtualizer(
                    0,
                    audioSessionId
                )
            } catch (_: Exception) {
                null
            }

        loudnessEnhancer =
            try {
                LoudnessEnhancer(
                    audioSessionId
                )
            } catch (_: Exception) {
                null
            }

        bassSupported.value =
            try {
                bassBoost?.strengthSupported == true
            } catch (_: Exception) {
                false
            }

        virtualizerSupported.value =
            try {
                virtualizer?.strengthSupported == true
            } catch (_: Exception) {
                false
            }

        applyBass()
        applyVirtualizer()
        refreshLoudness()
    }

    fun setBassEnabled(
        context: Context,
        value: Boolean
    ) {
        bassEnabled.value =
            value

        AudioEffectsPreferences.setBassEnabled(
            context,
            value
        )

        applyBass()
    }

    fun setBassStrength(
        context: Context,
        value: Int
    ) {
        bassStrength.value =
            value.coerceIn(
                0,
                1000
            )

        AudioEffectsPreferences.setBassStrength(
            context,
            value
        )

        applyBass()
    }

    fun setVirtualizerEnabled(
        context: Context,
        value: Boolean
    ) {
        virtualizerEnabled.value =
            value

        AudioEffectsPreferences.setVirtualizerEnabled(
            context,
            value
        )

        applyVirtualizer()
    }

    fun setVirtualizerStrength(
        context: Context,
        value: Int
    ) {
        virtualizerStrength.value =
            value.coerceIn(
                0,
                1000
            )

        AudioEffectsPreferences.setVirtualizerStrength(
            context,
            value
        )

        applyVirtualizer()
    }

    fun setLoudnessBoostDb(
        context: Context,
        value: Float
    ) {
        loudnessBoostDb.value =
            value.coerceIn(
                0f,
                12f
            )

        AudioEffectsPreferences.setLoudnessBoostDb(
            context,
            value
        )

        refreshLoudness()
    }

    fun refreshLoudness() {
        val enhancer =
            loudnessEnhancer
                ?: return

        val replayGainBoost =
            AudioProcessingState.replayGainDb.value
                .coerceAtLeast(
                    0f
                )

        val totalDb =
            (
                    loudnessBoostDb.value +
                            replayGainBoost
                    ).coerceIn(
                    0f,
                    24f
                )

        try {
            enhancer.setTargetGain(
                (totalDb * 100f).roundToInt()
            )

            enhancer.enabled =
                totalDb > 0f
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }

        try {
            virtualizer?.release()
        } catch (_: Exception) {
        }

        try {
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }

        bassBoost =
            null

        virtualizer =
            null

        loudnessEnhancer =
            null

        attachedSessionId =
            0
    }

    private fun applyBass() {
        val effect =
            bassBoost
                ?: return

        try {
            if (
                bassSupported.value
            ) {
                effect.setStrength(
                    bassStrength.value.toShort()
                )
            }

            effect.enabled =
                bassEnabled.value
        } catch (_: Exception) {
        }
    }

    private fun applyVirtualizer() {
        val effect =
            virtualizer
                ?: return

        try {
            if (
                virtualizerSupported.value
            ) {
                effect.setStrength(
                    virtualizerStrength.value.toShort()
                )
            }

            effect.enabled =
                virtualizerEnabled.value
        } catch (_: Exception) {
        }
    }
}