package com.keiranhaas.auralarc.storage

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object PowerSavingPreferences {

    private const val PREFS_NAME = "power_saving_prefs"
    private const val KEY_ENABLED = "enabled"

    private val _enabledState = mutableStateOf(false)

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            _enabledState.value = context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getBoolean(KEY_ENABLED, false)

            initialized = true
        }
    }

    fun isEnabled(): Boolean = _enabledState.value

    fun setEnabled(context: Context, enabled: Boolean) {
        _enabledState.value = enabled
        initialized = true

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (enabled) {
            AppearancePreferences.setPageAnimationsEnabled(
                context,
                false
            )
        }
    }
}