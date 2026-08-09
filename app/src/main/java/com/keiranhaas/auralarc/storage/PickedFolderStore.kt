package com.keiranhaas.auralarc.storage

import android.content.Context
import android.net.Uri
import org.json.JSONArray

object PickedFolderStore {

    private const val PREFS_NAME =
        "picked_folder_store"

    private const val KEY_FOLDER_URIS =
        "folder_uris"

    fun getFolderUris(
        context: Context
    ): List<String> {
        val rawJson =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            ).getString(
                KEY_FOLDER_URIS,
                "[]"
            ) ?: "[]"

        return try {
            val array =
                JSONArray(
                    rawJson
                )

            val folders =
                mutableListOf<String>()

            for (
            index in 0 until array.length()
            ) {
                val value =
                    array.optString(
                        index,
                        ""
                    )

                if (
                    value.isNotBlank()
                ) {
                    folders.add(
                        value
                    )
                }
            }

            folders
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addFolderUri(
        context: Context,
        uri: Uri
    ) {
        val folders =
            getFolderUris(
                context
            ).toMutableList()

        val value =
            uri.toString()

        if (
            !folders.contains(
                value
            )
        ) {
            folders.add(
                value
            )
        }

        saveFolders(
            context,
            folders
        )
    }

    fun removeFolderUri(
        context: Context,
        uriString: String
    ) {
        val folders =
            getFolderUris(
                context
            ).filterNot {
                it == uriString
            }

        saveFolders(
            context,
            folders
        )
    }

    fun clear(
        context: Context
    ) {
        saveFolders(
            context,
            emptyList()
        )
    }

    private fun saveFolders(
        context: Context,
        folders: List<String>
    ) {
        val array =
            JSONArray()

        folders.forEach { folder ->
            array.put(
                folder
            )
        }

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(
                KEY_FOLDER_URIS,
                array.toString()
            )
            .apply()
    }
}