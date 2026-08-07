package com.example.auralarc.storage

import android.content.Context
import com.example.auralarc.data.LibrarySource

object LibrarySourcePreferences {

    private const val PREFS_NAME =
        "library_source_prefs"

    private const val KEY_LIBRARY_SOURCE =
        "library_source"

    fun saveLibrarySource(
        context: Context,
        source: LibrarySource
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_LIBRARY_SOURCE,
                source.name
            )
            .apply()
    }

    fun getLibrarySource(
        context: Context
    ): LibrarySource {
        val rawValue =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
                .getString(
                    KEY_LIBRARY_SOURCE,
                    LibrarySource.LOCAL.name
                )

        return try {
            LibrarySource.valueOf(
                rawValue ?: LibrarySource.LOCAL.name
            )
        } catch (e: Exception) {
            LibrarySource.LOCAL
        }
    }
}