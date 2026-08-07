package com.example.auralarc.navidrome

import android.content.Context
import android.util.Log
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.data.Playlist

object NavidromePlaylistManager {

    fun loadPlaylists(
        context: Context
    ): List<Playlist> {
        return loadPlaylistsOrNull(
            context
        ) ?: emptyList()
    }

    fun loadPlaylistsOrNull(
        context: Context
    ): List<Playlist>? {
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
            NavidromeClient(
                credentials
            ).getRemotePlaylists()
        } catch (e: Exception) {
            Log.e(
                "AuralArc",
                "Failed to load Navidrome playlists",
                e
            )

            null
        }
    }

    fun loadPlaylistDetail(
        context: Context,
        playlistId: String
    ): Pair<Playlist, List<MusicTrack>>? {
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

            return null
        }

        return try {
            NavidromeClient(
                credentials
            ).getRemotePlaylistDetail(
                playlistId
            )
        } catch (e: Exception) {
            Log.e(
                "AuralArc",
                "Failed to load Navidrome playlist detail",
                e
            )

            null
        }
    }
}