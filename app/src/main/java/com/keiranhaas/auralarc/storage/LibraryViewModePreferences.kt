package com.keiranhaas.auralarc.storage

import android.content.Context
import com.keiranhaas.auralarc.ui.LibraryMode
import com.keiranhaas.auralarc.ui.LibraryViewMode

object LibraryViewModePreferences {

    private const val PREFS_NAME =
        "library_view_mode_preferences"

    private fun keyFor(
        mode: LibraryMode
    ): String {
        return "view_mode_${mode.name}"
    }

    fun get(
        context: Context,
        mode: LibraryMode
    ): LibraryViewMode {
        val savedValue =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getString(
                    keyFor(
                        mode
                    ),
                    null
                )

        return try {
            if (
                savedValue.isNullOrBlank()
            ) {
                LibraryViewMode.LIST
            } else {
                LibraryViewMode.valueOf(
                    savedValue
                )
            }
        } catch (_: Exception) {
            LibraryViewMode.LIST
        }
    }

    fun set(
        context: Context,
        mode: LibraryMode,
        viewMode: LibraryViewMode
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                keyFor(
                    mode
                ),
                viewMode.name
            )
            .apply()
    }
}