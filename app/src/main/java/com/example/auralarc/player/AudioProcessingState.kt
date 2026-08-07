package com.example.auralarc.player

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.storage.AdvancedAudioPreferences
import kotlin.math.pow

object AudioProcessingState {

    val replayGainDb =
        mutableStateOf(
            0f
        )

    val replayGainMultiplier =
        mutableStateOf(
            1f
        )

    val replayGainLabel =
        mutableStateOf(
            "ReplayGain off"
        )

    val replayGainSourceLabel =
        mutableStateOf(
            "None"
        )

    val resamplerLabel =
        mutableStateOf(
            "Android / ExoPlayer default"
        )

    val ditherLabel =
        mutableStateOf(
            "None"
        )

    val dvcLabel =
        mutableStateOf(
            "DVC not active"
        )

    val outputPluginLabel =
        mutableStateOf(
            "AudioTrack / Android default"
        )

    fun reset() {
        replayGainMultiplier.value = 1f
    }

    fun refresh(
        context: Context,
        track: MusicTrack?
    ) {
        refreshReplayGain(
            context = context,
            track = track
        )

        refreshSavedProcessingLabels(
            context = context
        )
    }

    private fun refreshReplayGain(
        context: Context,
        track: MusicTrack?
    ) {
        val mode =
            AdvancedAudioPreferences.getReplayGainMode(
                context
            )

        if (
            mode == AdvancedAudioPreferences.REPLAY_GAIN_OFF
        ) {
            replayGainDb.value =
                0f

            replayGainMultiplier.value =
                1f

            replayGainLabel.value =
                "ReplayGain off"

            replayGainSourceLabel.value =
                "None"

            return
        }

        val source =
            AdvancedAudioPreferences.getReplayGainSource(
                context
            )

        val replayGainValue =
            when (
                source
            ) {
                AdvancedAudioPreferences.REPLAY_GAIN_SOURCE_TRACK ->
                    track?.replayGainTrackGain

                else ->
                    track?.replayGainAlbumGain
            }

        val preamp =
            AdvancedAudioPreferences.getReplayGainPreamp(
                context
            )

        val noRgPreamp =
            AdvancedAudioPreferences.getNoReplayGainPreamp(
                context
            )

        val baseGain =
            replayGainValue ?: noRgPreamp

        var finalGainDb =
            baseGain + preamp

        if (
            mode == AdvancedAudioPreferences.REPLAY_GAIN_PREVENT_CLIPPING
        ) {
            val peak =
                when (
                    source
                ) {
                    AdvancedAudioPreferences.REPLAY_GAIN_SOURCE_TRACK ->
                        track?.replayGainTrackPeak

                    else ->
                        track?.replayGainAlbumPeak
                }

            if (
                peak != null &&
                peak > 0f
            ) {
                val multiplierWithoutClipLimit =
                    dbToLinear(
                        finalGainDb
                    )

                val predictedPeak =
                    peak * multiplierWithoutClipLimit

                if (
                    predictedPeak > 1f
                ) {
                    val maxSafeGain =
                        linearToDb(
                            1f / peak
                        )

                    finalGainDb =
                        minOf(
                            finalGainDb,
                            maxSafeGain
                        )
                }
            }
        }

        replayGainDb.value =
            finalGainDb

        replayGainMultiplier.value =
            dbToLinear(
                finalGainDb
            )

        replayGainSourceLabel.value =
            if (
                source == AdvancedAudioPreferences.REPLAY_GAIN_SOURCE_TRACK
            ) {
                "Track"
            } else {
                "Album"
            }

        replayGainLabel.value =
            if (
                replayGainValue == null
            ) {
                "No ReplayGain tag: using ${formatDb(noRgPreamp)} no-RG preamp + ${formatDb(preamp)} RG preamp"
            } else {
                "${replayGainSourceLabel.value} ReplayGain ${formatDb(replayGainValue)} + ${formatDb(preamp)} preamp = ${formatDb(finalGainDb)}"
            }
    }

    private fun refreshSavedProcessingLabels(
        context: Context
    ) {
        resamplerLabel.value =
            when (
                AdvancedAudioPreferences.getResamplerType(
                    context
                )
            ) {
                AdvancedAudioPreferences.RESAMPLER_SOX_VERY_HIGH_QUALITY ->
                    "SoX - very high quality (${AdvancedAudioPreferences.getResamplerCutoff(context).toInt()}% cutoff)"

                else ->
                    "SW - high quality (${AdvancedAudioPreferences.getResamplerCutoff(context).toInt()}% cutoff)"
            }

        ditherLabel.value =
            AdvancedAudioPreferences.getDither(
                context
            )

        dvcLabel.value =
            if (
                AdvancedAudioPreferences.getDvcEnabled(
                    context
                )
            ) {
                "DVC preference enabled, preamp reduction ${AdvancedAudioPreferences.getDvcPreampReduction(context).toInt()} dB"
            } else {
                "DVC disabled"
            }

        val selectedOutput =
            AdvancedAudioPreferences.getSelectedOutputPlugin(
                context
            )

        outputPluginLabel.value =
            AdvancedAudioPreferences.outputPlugins
                .firstOrNull {
                    it.first == selectedOutput
                }
                ?.second
                ?: "AudioTrack / Android default"
    }

    private fun dbToLinear(
        db: Float
    ): Float {
        return 10.0.pow(
            db / 20.0
        ).toFloat()
    }

    private fun linearToDb(
        linear: Float
    ): Float {
        if (
            linear <= 0f
        ) {
            return -120f
        }

        return (
                20.0 *
                        kotlin.math.log10(
                            linear.toDouble()
                        )
                ).toFloat()
    }

    private fun formatDb(
        value: Float
    ): String {
        return if (
            value >= 0f
        ) {
            "+%.2f dB".format(
                value
            )
        } else {
            "%.2f dB".format(
                value
            )
        }
    }
}