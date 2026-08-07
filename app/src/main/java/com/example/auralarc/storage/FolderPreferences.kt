package com.example.auralarc.storage

import android.content.Context

object FolderPreferences {

    private const val PREFS_NAME =
        "folder_prefs"

    private const val KEY_FOLDERS =
        "folders"

    fun saveFolders(
        context: Context,
        folders: List<String>
    ) {

        val cleanedFolders =
            folders.filter { folder ->
                folder in FolderManager.allowedFolders
            }
                .toSet()

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putStringSet(
                KEY_FOLDERS,
                cleanedFolders
            )
            .apply()
    }

    fun loadFolders(
        context: Context
    ): MutableList<String> {

        val savedFolders =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
                .getStringSet(
                    KEY_FOLDERS,
                    null
                )

        val cleanedFolders =
            savedFolders
                ?.filter { folder ->
                    folder in FolderManager.allowedFolders
                }
                ?.toMutableList()

        return if (
            cleanedFolders.isNullOrEmpty()
        ) {
            FolderManager.allowedFolders.toMutableList()
        } else {
            cleanedFolders
        }
    }
}