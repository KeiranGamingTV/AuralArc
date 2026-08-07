package com.example.auralarc.navidrome

import android.content.Context
import android.util.Log
import com.example.auralarc.data.MusicTrack

object NavidromeLibraryManager {

    fun loadLibrary(
        context: Context
    ): List<MusicTrack> {
        return loadLibraryOrNull(
            context
        ) ?: emptyList()
    }

    fun loadLibraryOrNull(
        context: Context
    ): List<MusicTrack>? {
        val credentials =
            NavidromePreferences.getCredentials(
                context
            )

        if (
            credentials == null
        ) {
            Log.d(
                "AuralArc",
                "Navidrome credentials are not set."
            )

            return emptyList()
        }

        return try {
            val client =
                NavidromeClient(
                    credentials
                )

            val tracks =
                client.getAllSongs()

            Log.d(
                "AuralArc",
                "Navidrome returned ${tracks.size} tracks"
            )

            tracks
        } catch (e: Exception) {
            Log.e(
                "AuralArc",
                "Failed to load Navidrome library",
                e
            )

            null
        }
    }
}