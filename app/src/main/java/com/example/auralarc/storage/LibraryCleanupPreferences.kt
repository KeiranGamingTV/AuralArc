package com.example.auralarc.storage

import android.content.Context

object LibraryCleanupPreferences {

    private const val PREFS_NAME =
        "library_cleanup_prefs"

    private const val KEY_HIDE_UNKNOWN_ARTIST =
        "hide_unknown_artist"

    private const val KEY_HIDE_SHORT_TRACKS =
        "hide_short_tracks"

    private const val KEY_MIN_DURATION_SECONDS =
        "min_duration_seconds"

    private const val KEY_HIDE_ZERO_DURATION =
        "hide_zero_duration"

    private const val KEY_DEDUPLICATE =
        "deduplicate"

    fun getHideUnknownArtist(
        context: Context
    ): Boolean {
        return prefs(
            context
        ).getBoolean(
            KEY_HIDE_UNKNOWN_ARTIST,
            false
        )
    }

    fun setHideUnknownArtist(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                KEY_HIDE_UNKNOWN_ARTIST,
                enabled
            )
            .apply()
    }

    fun getHideShortTracks(
        context: Context
    ): Boolean {
        return prefs(
            context
        ).getBoolean(
            KEY_HIDE_SHORT_TRACKS,
            false
        )
    }

    fun setHideShortTracks(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                KEY_HIDE_SHORT_TRACKS,
                enabled
            )
            .apply()
    }

    fun getMinDurationSeconds(
        context: Context
    ): Int {
        return prefs(
            context
        ).getInt(
            KEY_MIN_DURATION_SECONDS,
            30
        )
    }

    fun setMinDurationSeconds(
        context: Context,
        seconds: Int
    ) {
        prefs(
            context
        ).edit()
            .putInt(
                KEY_MIN_DURATION_SECONDS,
                seconds.coerceAtLeast(
                    1
                )
            )
            .apply()
    }

    fun getHideZeroDuration(
        context: Context
    ): Boolean {
        return prefs(
            context
        ).getBoolean(
            KEY_HIDE_ZERO_DURATION,
            true
        )
    }

    fun setHideZeroDuration(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                KEY_HIDE_ZERO_DURATION,
                enabled
            )
            .apply()
    }

    fun getDeduplicate(
        context: Context
    ): Boolean {
        return prefs(
            context
        ).getBoolean(
            KEY_DEDUPLICATE,
            true
        )
    }

    fun setDeduplicate(
        context: Context,
        enabled: Boolean
    ) {
        prefs(
            context
        ).edit()
            .putBoolean(
                KEY_DEDUPLICATE,
                enabled
            )
            .apply()
    }

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
}