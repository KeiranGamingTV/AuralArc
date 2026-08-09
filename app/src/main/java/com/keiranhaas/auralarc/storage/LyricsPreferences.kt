package com.keiranhaas.auralarc.storage

import android.content.Context
import android.net.Uri

object LyricsPreferences {

    private const val PREFERENCES_NAME =
        "lyrics_preferences"

    private const val KEY_ENABLE_DUET_LYRICS =
        "enable_duet_lyrics"

    private const val KEY_LYRICS_FOLDER_URI =
        "lyrics_folder_uri"

    fun getDuetLyricsEnabled(
        context: Context
    ): Boolean {
        return context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                KEY_ENABLE_DUET_LYRICS,
                false
            )
    }

    fun setDuetLyricsEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLE_DUET_LYRICS,
                enabled
            )
            .apply()
    }

    fun getLyricsFolderUri(
        context: Context
    ): String? {
        return context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_LYRICS_FOLDER_URI,
                null
            )
    }

    fun setLyricsFolderUri(
        context: Context,
        uri: String
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LYRICS_FOLDER_URI,
                uri
            )
            .apply()
    }

    fun clearLyricsFolderUri(
        context: Context
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_LYRICS_FOLDER_URI
            )
            .apply()
    }

    fun migrateLegacyLyricsFolderToPickedFolders(
        context: Context
    ) {
        val legacyFolderUri =
            getLyricsFolderUri(
                context
            )
                ?: return

        val parsedUri =
            try {
                Uri.parse(
                    legacyFolderUri
                )
            } catch (_: Exception) {
                null
            }
                ?: return

        /*
         * Move the previously selected Lyrics Folder into the
         * existing general-purpose music-folder list.
         *
         * The persisted Android URI permission remains valid.
         */
        PickedFolderStore.addFolderUri(
            context = context.applicationContext,
            uri = parsedUri
        )

        clearLyricsFolderUri(
            context
        )
    }
}