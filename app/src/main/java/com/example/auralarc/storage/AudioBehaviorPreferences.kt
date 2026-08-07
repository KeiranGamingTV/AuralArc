package com.example.auralarc.storage

import android.content.Context

object AudioBehaviorPreferences {

    private const val PREFS_NAME =
        "audio_behavior_prefs"

    private const val KEY_STOP_WHEN_APP_CLOSED =
        "stop_when_app_closed"

    private const val KEY_PAUSE_ON_HEADPHONE_DISCONNECT =
        "pause_on_headphone_disconnect"

    private const val KEY_USE_AUDIO_FOCUS =
        "use_audio_focus"

    private const val KEY_SMART_SHUFFLE_LEVEL =
        "smart_shuffle_level"

    fun getStopWhenAppClosed(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_STOP_WHEN_APP_CLOSED,
            true
        )
    }

    fun setStopWhenAppClosed(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_STOP_WHEN_APP_CLOSED,
                enabled
            )
            .apply()
    }

    fun getPauseOnHeadphoneDisconnect(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_PAUSE_ON_HEADPHONE_DISCONNECT,
            true
        )
    }

    fun setPauseOnHeadphoneDisconnect(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_PAUSE_ON_HEADPHONE_DISCONNECT,
                enabled
            )
            .apply()
    }

    fun getUseAudioFocus(
        context: Context
    ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_USE_AUDIO_FOCUS,
            true
        )
    }

    fun setUseAudioFocus(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                KEY_USE_AUDIO_FOCUS,
                enabled
            )
            .apply()
    }

    fun getSmartShuffleLevel(
        context: Context
    ): Int {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getInt(
            KEY_SMART_SHUFFLE_LEVEL,
            2
        ).coerceIn(
            0,
            4
        )
    }

    fun setSmartShuffleLevel(
        context: Context,
        level: Int
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putInt(
                KEY_SMART_SHUFFLE_LEVEL,
                level.coerceIn(
                    0,
                    4
                )
            )
            .apply()
    }
}