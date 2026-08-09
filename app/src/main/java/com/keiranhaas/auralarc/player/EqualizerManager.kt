package com.keiranhaas.auralarc.player

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.keiranhaas.auralarc.storage.EqualizerPreferences
import kotlin.math.roundToInt

object EqualizerManager {

    private var equalizer: Equalizer? =
        null

    private var attachedSessionId =
        0

    val availableBandsHz =
        mutableStateListOf<Int>()

    val bandGainsDb =
        mutableStateListOf<Float>()

    val enabled =
        mutableStateOf(false)

    val statusText =
        mutableStateOf("Equalizer not attached")

    fun attachToAudioSession(
        context: Context,
        audioSessionId: Int
    ) {
        val appContext =
            context.applicationContext

        if (
            audioSessionId == 0
        ) {
            statusText.value =
                "Equalizer waiting for audio session"

            return
        }

        if (
            attachedSessionId == audioSessionId &&
            equalizer != null
        ) {
            return
        }

        release()

        attachedSessionId =
            audioSessionId

        try {
            val newEqualizer =
                Equalizer(
                    0,
                    audioSessionId
                )

            equalizer =
                newEqualizer

            val bandCount =
                newEqualizer.numberOfBands
                    .toInt()
                    .coerceAtMost(
                        32
                    )

            availableBandsHz.clear()
            bandGainsDb.clear()

            val savedEnabled =
                EqualizerPreferences.getEnabled(
                    appContext
                )

            val savedGains =
                EqualizerPreferences.getBandGains(
                    appContext
                )

            for (
            index in 0 until bandCount
            ) {
                val centerHz =
                    newEqualizer.getCenterFreq(
                        index.toShort()
                    ) / 1000

                availableBandsHz.add(
                    centerHz
                )

                val gain =
                    savedGains.getOrNull(
                        index
                    ) ?: 0f

                bandGainsDb.add(
                    gain.coerceIn(
                        -12f,
                        12f
                    )
                )
            }

            newEqualizer.enabled =
                savedEnabled

            enabled.value =
                savedEnabled

            bandGainsDb.forEachIndexed { index, gain ->
                setBandGainInternal(
                    index = index,
                    gainDb = gain
                )
            }

            statusText.value =
                "$bandCount bands active"
        } catch (e: Exception) {
            equalizer =
                null

            attachedSessionId =
                0

            availableBandsHz.clear()
            bandGainsDb.clear()

            enabled.value =
                false

            statusText.value =
                "Equalizer unavailable on this device"
        }
    }

    fun setEnabled(
        context: Context,
        value: Boolean
    ) {
        enabled.value =
            value

        EqualizerPreferences.setEnabled(
            context,
            value
        )

        try {
            equalizer?.enabled =
                value
        } catch (_: Exception) {
        }
    }

    fun setBandGain(
        context: Context,
        index: Int,
        gainDb: Float
    ) {
        setBandGainInternal(
            index = index,
            gainDb = gainDb
        )

        EqualizerPreferences.setBandGains(
            context.applicationContext,
            bandGainsDb.toList()
        )
    }

    private fun setBandGainInternal(
        index: Int,
        gainDb: Float
    ) {
        if (
            index !in bandGainsDb.indices
        ) {
            return
        }

        val safeGain =
            gainDb.coerceIn(
                -12f,
                12f
            )

        bandGainsDb[index] =
            safeGain

        val activeEqualizer =
            equalizer

        if (
            activeEqualizer != null
        ) {
            try {
                val range =
                    activeEqualizer.bandLevelRange

                val minMb =
                    range[0].toInt()

                val maxMb =
                    range[1].toInt()

                val requestedMb =
                    (safeGain * 100f).roundToInt()
                        .coerceIn(
                            minMb,
                            maxMb
                        )

                activeEqualizer.setBandLevel(
                    index.toShort(),
                    requestedMb.toShort()
                )
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }

        equalizer =
            null

        attachedSessionId =
            0

        statusText.value =
            "Equalizer released"
    }
}