package com.example.auralarc.storage

import android.content.Context

object TodaysPicksPreferences {

    private const val PREFS_NAME =
        "todays_picks_prefs"

    private const val KEY_ENABLED =
        "todays_picks_enabled"

    fun getEnabled(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_ENABLED,
            true
        )
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }
}