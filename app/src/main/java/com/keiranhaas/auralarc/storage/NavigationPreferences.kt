package com.keiranhaas.auralarc.storage

import android.content.Context
import com.keiranhaas.auralarc.ui.LibraryMode
import com.keiranhaas.auralarc.ui.LibraryRootTab

object NavigationPreferences {

    private const val PREFS_NAME =
        "navigation_preferences"

    private const val KEY_LAST_ROOT_TAB =
        "last_root_tab"

    private const val KEY_LAST_LIBRARY_MODE =
        "last_library_mode"

    fun getLastRootTab(
        context: Context
    ): LibraryRootTab {
        val savedValue =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_LAST_ROOT_TAB,
                    null
                )

        return try {
            if (
                savedValue.isNullOrBlank()
            ) {
                LibraryRootTab.HOME
            } else {
                LibraryRootTab.valueOf(
                    savedValue
                )
            }
        } catch (_: Exception) {
            LibraryRootTab.HOME
        }
    }

    fun setLastRootTab(
        context: Context,
        tab: LibraryRootTab
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LAST_ROOT_TAB,
                tab.name
            )
            .apply()
    }

    fun getLastLibraryMode(
        context: Context
    ): LibraryMode {
        val preferences =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        val savedValue =
            preferences.getString(
                KEY_LAST_LIBRARY_MODE,
                null
            )

        if (
            !savedValue.isNullOrBlank()
        ) {
            return try {
                LibraryMode.valueOf(
                    savedValue
                )
            } catch (_: Exception) {
                LibraryMode.ALBUMS
            }
        }

        /*
         * On the first launch after this update, use the old
         * Default Library Tab selection as the starting point.
         * After that, the user's most recently selected category
         * becomes the saved value.
         */
        return try {
            LibraryMode.valueOf(
                AppearancePreferences.getDefaultTab(
                    context
                )
            )
        } catch (_: Exception) {
            LibraryMode.ALBUMS
        }
    }

    fun setLastLibraryMode(
        context: Context,
        mode: LibraryMode
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LAST_LIBRARY_MODE,
                mode.name
            )
            .apply()
    }
}