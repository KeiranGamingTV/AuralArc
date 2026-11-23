package com.keiranhaas.auralarc.storage

import android.content.Context

object CrossfadePreferences {

    private const val PREFS_NAME =
        "auralarc_crossfade_preferences"

    const val MODE_FADE =
        0

    const val MODE_OVERLAP =
        1

    private fun prefs(
        context: Context
    ) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getEnabled(
        context: Context
    ): Boolean =
        prefs(
            context
        ).getBoolean(
            "enabled",
            false
        )

    fun setEnabled(
        context: Context,
        value: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                "enabled",
                value
            )
            .apply()
    }

    fun getMode(
        context: Context
    ): Int =
        prefs(
            context
        ).getInt(
            "mode",
            MODE_OVERLAP
        )

    fun setMode(
        context: Context,
        value: Int
    ) {
        prefs(
            context
        ).edit()
            .putInt(
                "mode",
                value
            )
            .apply()
    }

    fun getSeconds(
        context: Context
    ): Int =
        prefs(
            context
        ).getInt(
            "seconds",
            4
        ).coerceIn(
            1,
            12
        )

    fun setSeconds(
        context: Context,
        value: Int
    ) {
        prefs(
            context
        ).edit()
            .putInt(
                "seconds",
                value.coerceIn(
                    1,
                    12
                )
            )
            .apply()
    }
}