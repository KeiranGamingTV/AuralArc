package com.keiranhaas.auralarc.storage

import android.content.Context

object AppearancePreferences {

    private const val PREFS_NAME =
        "appearance_prefs"

    private const val KEY_COMPACT_ROWS =
        "compact_rows"

    private const val KEY_SHOW_ARTWORK =
        "show_artwork"

    private const val KEY_SHOW_DURATION =
        "show_duration"

    private const val KEY_DEFAULT_TAB =
        "default_tab"

    fun getCompactRows(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_COMPACT_ROWS,
            false
        )
    }

    fun setCompactRows(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_COMPACT_ROWS,
                enabled
            )
            .apply()
    }

    fun getShowArtwork(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_SHOW_ARTWORK,
            true
        )
    }

    fun setShowArtwork(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_SHOW_ARTWORK,
                enabled
            )
            .apply()
    }

    fun getShowDuration(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_SHOW_DURATION,
            true
        )
    }

    fun setShowDuration(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_SHOW_DURATION,
                enabled
            )
            .apply()
    }

    fun getDefaultTab(
        context: Context
    ): String {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString(
            KEY_DEFAULT_TAB,
            "ALBUMS"
        ) ?: "ALBUMS"
    }

    fun setDefaultTab(
        context: Context,
        tabName: String
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(
                KEY_DEFAULT_TAB,
                tabName
            )
            .apply()
    }
}