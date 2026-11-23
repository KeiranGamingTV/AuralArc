package com.keiranhaas.auralarc.storage

import android.content.Context

object AudioEffectsPreferences {

    private const val PREFS_NAME =
        "auralarc_audio_effects_preferences"

    private fun prefs(
        context: Context
    ) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getBassEnabled(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "bass_enabled",
            false
        )

    fun setBassEnabled(
        context: Context,
        value: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "bass_enabled",
                value
            )
            .apply()
    }

    fun getBassStrength(
        context: Context
    ): Int =
        prefs(
            context
        ).getInt(
            "bass_strength",
            500
        ).coerceIn(
            0,
            1000
        )

    fun setBassStrength(
        context: Context,
        value: Int
    ) {
        prefs(
            context
        ).edit()
            .putInt(
                "bass_strength",
                value.coerceIn(
                    0,
                    1000
                )
            )
            .apply()
    }

    fun getVirtualizerEnabled(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "virtualizer_enabled",
            false
        )

    fun setVirtualizerEnabled(
        context: Context,
        value: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "virtualizer_enabled",
                value
            )
            .apply()
    }

    fun getVirtualizerStrength(
        context: Context
    ): Int =
        prefs(
            context
        ).getInt(
            "virtualizer_strength",
            500
        ).coerceIn(
            0,
            1000
        )

    fun setVirtualizerStrength(
        context: Context,
        value: Int
    ) {
        prefs(
            context
        ).edit()
            .putInt(
                "virtualizer_strength",
                value.coerceIn(
                    0,
                    1000
                )
            )
            .apply()
    }

    fun getLoudnessBoostDb(
        context: Context
    ): Float =
        prefs(
            context
        ).getFloat(
            "loudness_boost_db",
            0f
        ).coerceIn(
            0f,
            12f
        )

    fun setLoudnessBoostDb(
        context: Context,
        value: Float
    ) {
        prefs(
            context
        ).edit()
            .putFloat(
                "loudness_boost_db",
                value.coerceIn(
                    0f,
                    12f
                )
            )
            .apply()
    }
}