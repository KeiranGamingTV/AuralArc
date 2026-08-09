package com.keiranhaas.auralarc.storage

import android.content.Context

object AdvancedAudioPreferences {

    private const val PREFS_NAME =
        "advanced_audio_preferences"

    const val REPLAY_GAIN_OFF =
        "off"

    const val REPLAY_GAIN_APPLY =
        "apply"

    const val REPLAY_GAIN_PREVENT_CLIPPING =
        "prevent_clipping"

    const val REPLAY_GAIN_SOURCE_ALBUM =
        "album"

    const val REPLAY_GAIN_SOURCE_TRACK =
        "track"

    const val RESAMPLER_SW_HIGH_QUALITY =
        "sw_high_quality"

    const val RESAMPLER_SOX_VERY_HIGH_QUALITY =
        "sox_very_high_quality"

    const val OUTPUT_OPENSL =
        "opensl"

    const val OUTPUT_AUDIOTRACK =
        "audiotrack"

    const val OUTPUT_HI_RES =
        "hi_res"

    const val OUTPUT_AAUDIO =
        "aaudio"

    const val OUTPUT_CHROMECAST =
        "chromecast"

    val ditherOptions =
        listOf(
            "None",
            "Rectangular",
            "Triangular",
            "Triangular with High Pass",
            "F-weighted noise shaping",
            "Modified-e-weighted noise shaping",
            "Improved-e-weighted noise shaping",
            "Shibata noise shaping",
            "Low Shibata noise shaping"
        )

    val outputPlugins =
        listOf(
            OUTPUT_OPENSL to "OpenSL ES Output",
            OUTPUT_AUDIOTRACK to "AudioTrack Output",
            OUTPUT_HI_RES to "Hi-Res Output",
            OUTPUT_AAUDIO to "AAudio Output",
            OUTPUT_CHROMECAST to "Chromecast Output"
        )

    val outputDeviceTypes =
        listOf(
            "Wired Headset/AUX",
            "Speaker",
            "Bluetooth",
            "USB DAC",
            "Other Output Devices"
        )

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getReplayGainMode(
        context: Context
    ): String =
        prefs(
            context
        ).getString(
            "replay_gain_mode",
            REPLAY_GAIN_OFF
        ) ?: REPLAY_GAIN_OFF

    fun setReplayGainMode(
        context: Context,
        mode: String
    ) {
        prefs(
            context
        ).edit()
            .putString(
                "replay_gain_mode",
                mode
            )
            .apply()
    }

    fun getReplayGainSource(
        context: Context
    ): String =
        prefs(
            context
        ).getString(
            "replay_gain_source",
            REPLAY_GAIN_SOURCE_ALBUM
        ) ?: REPLAY_GAIN_SOURCE_ALBUM

    fun setReplayGainSource(
        context: Context,
        source: String
    ) {
        prefs(
            context
        ).edit()
            .putString(
                "replay_gain_source",
                source
            )
            .apply()
    }

    fun getReplayGainPreamp(
        context: Context
    ): Float =
        prefs(
            context
        ).getFloat(
            "replay_gain_preamp",
            0f
        )

    fun setReplayGainPreamp(
        context: Context,
        value: Float
    ) {
        prefs(
            context
        ).edit()
            .putFloat(
                "replay_gain_preamp",
                value.coerceIn(
                    -12f,
                    12f
                )
            )
            .apply()
    }

    fun getNoReplayGainPreamp(
        context: Context
    ): Float =
        prefs(
            context
        ).getFloat(
            "no_replay_gain_preamp",
            0f
        )

    fun setNoReplayGainPreamp(
        context: Context,
        value: Float
    ) {
        prefs(
            context
        ).edit()
            .putFloat(
                "no_replay_gain_preamp",
                value.coerceIn(
                    -12f,
                    12f
                )
            )
            .apply()
    }

    fun getResumeAfterCall(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "resume_after_call",
            true
        )

    fun setResumeAfterCall(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "resume_after_call",
                enabled
            )
            .apply()
    }

    fun getResumeOnFocusGain(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "resume_on_focus_gain",
            true
        )

    fun setResumeOnFocusGain(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "resume_on_focus_gain",
                enabled
            )
            .apply()
    }

    fun getDuckVolume(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "duck_volume",
            true
        )

    fun setDuckVolume(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "duck_volume",
                enabled
            )
            .apply()
    }

    fun getPermanentAudioFocusChange(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "permanent_audio_focus_change",
            true
        )

    fun setPermanentAudioFocusChange(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "permanent_audio_focus_change",
                enabled
            )
            .apply()
    }

    fun getResamplerType(
        context: Context
    ): String =
        prefs(
            context
        ).getString(
            "resampler_type",
            RESAMPLER_SW_HIGH_QUALITY
        ) ?: RESAMPLER_SW_HIGH_QUALITY

    fun setResamplerType(
        context: Context,
        value: String
    ) {
        prefs(
            context
        ).edit()
            .putString(
                "resampler_type",
                value
            )
            .apply()
    }

    fun getResamplerCutoff(
        context: Context
    ): Float =
        prefs(
            context
        ).getFloat(
            "resampler_cutoff",
            97f
        )

    fun setResamplerCutoff(
        context: Context,
        value: Float
    ) {
        prefs(
            context
        ).edit()
            .putFloat(
                "resampler_cutoff",
                value.coerceIn(
                    80f,
                    100f
                )
            )
            .apply()
    }

    fun getDither(
        context: Context
    ): String =
        prefs(
            context
        ).getString(
            "dither",
            ditherOptions.first()
        ) ?: ditherOptions.first()

    fun setDither(
        context: Context,
        value: String
    ) {
        prefs(
            context
        ).edit()
            .putString(
                "dither",
                value
            )
            .apply()
    }

    fun getDvcEnabled(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "dvc_enabled",
            false
        )

    fun setDvcEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "dvc_enabled",
                enabled
            )
            .apply()
    }

    fun getNoDvcForBluetoothAbsoluteVolume(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "no_dvc_bluetooth_absolute_volume",
            true
        )

    fun setNoDvcForBluetoothAbsoluteVolume(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "no_dvc_bluetooth_absolute_volume",
                enabled
            )
            .apply()
    }

    fun getDvcPreampReduction(
        context: Context
    ): Float =
        prefs(
            context
        ).getFloat(
            "dvc_preamp_reduction",
            -6f
        )

    fun setDvcPreampReduction(
        context: Context,
        value: Float
    ) {
        prefs(
            context
        ).edit()
            .putFloat(
                "dvc_preamp_reduction",
                value.coerceIn(
                    -12f,
                    0f
                )
            )
            .apply()
    }

    fun getSelectedOutputPlugin(
        context: Context
    ): String =
        prefs(
            context
        ).getString(
            "selected_output_plugin",
            OUTPUT_AUDIOTRACK
        ) ?: OUTPUT_AUDIOTRACK

    fun setSelectedOutputPlugin(
        context: Context,
        plugin: String
    ) {
        prefs(
            context
        ).edit()
            .putString(
                "selected_output_plugin",
                plugin
            )
            .apply()
    }

    fun getOutputDeviceEnabled(
        context: Context,
        plugin: String,
        device: String
    ): Boolean {
        val defaultEnabled =
            when (
                device
            ) {
                "Speaker",
                "Wired Headset/AUX",
                "Other Output Devices" ->
                    true

                else ->
                    false
            }

        return prefs(
            context
        ).getBoolean(
            "output_${plugin}_${device}",
            defaultEnabled
        )
    }

    fun setOutputDeviceEnabled(
        context: Context,
        plugin: String,
        device: String,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "output_${plugin}_${device}",
                enabled
            )
            .apply()
    }

}