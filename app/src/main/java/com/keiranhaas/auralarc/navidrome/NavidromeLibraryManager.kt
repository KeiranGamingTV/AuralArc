package com.keiranhaas.auralarc.navidrome

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.keiranhaas.auralarc.data.MusicTrack

object NavidromeLibraryManager {

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

            val startTime =
                SystemClock.elapsedRealtime()

            val tracks =
                client.getAllSongs()

            val elapsedMilliseconds =
                SystemClock.elapsedRealtime() -
                        startTime

            Log.d(
                "AuralArc",
                "Navidrome returned ${tracks.size} tracks in ${elapsedMilliseconds}ms"
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