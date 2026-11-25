package com.keiranhaas.auralarc.navidrome

import android.content.Context
import android.util.Log
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import com.keiranhaas.auralarc.data.TrackIdentity
import com.keiranhaas.auralarc.storage.LibraryCacheStore

object NavidromePlaylistManager {

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

    fun createPlaylist(
        context: Context,
        name: String,
        tracks: List<MusicTrack> = emptyList()
    ): NavidromeApiResult {
        return runEdit(
            context
        ) { client ->
            client.createRemotePlaylist(
                name = name.trim(),
                songIds = navidromeSongIds(
                    tracks
                )
            )
        }
    }

    fun renamePlaylist(
        context: Context,
        playlistId: String,
        newName: String
    ): NavidromeApiResult {
        return runEdit(
            context
        ) { client ->
            client.renameRemotePlaylist(
                playlistId = playlistId,
                newName = newName.trim()
            )
        }
    }

    fun deletePlaylist(
        context: Context,
        playlistId: String
    ): NavidromeApiResult {
        return runEdit(
            context
        ) { client ->
            client.deleteRemotePlaylist(
                playlistId
            )
        }
    }

    fun addTracks(
        context: Context,
        playlistId: String,
        tracks: List<MusicTrack>
    ): NavidromeApiResult {
        return runEdit(
            context
        ) { client ->
            client.addSongsToRemotePlaylist(
                playlistId = playlistId,
                songIds = navidromeSongIds(
                    tracks
                )
            )
        }
    }

    fun removeTrackAt(
        context: Context,
        playlistId: String,
        index: Int
    ): NavidromeApiResult {
        return runEdit(
            context
        ) { client ->
            client.removeSongFromRemotePlaylist(
                playlistId = playlistId,
                songIndex = index
            )
        }
    }

    private fun navidromeSongIds(
        tracks: List<MusicTrack>
    ): List<String> {
        return tracks.mapNotNull { track ->
            TrackIdentity.navidromeSongId(
                track
            )
        }
    }

    private fun runEdit(
        context: Context,
        action: (NavidromeClient) -> Unit
    ): NavidromeApiResult {
        val credentials =
            NavidromePreferences.getCredentials(
                context
            ) ?: return NavidromeApiResult(
                success = false,
                message = "Navidrome login is not saved."
            )

        return try {
            action(
                NavidromeClient(
                    credentials
                )
            )

            LibraryCacheStore.clearNavidromePlaylists(
                context
            )

            NavidromeApiResult(
                success = true,
                message = "Done"
            )
        } catch (e: Exception) {
            Log.e(
                "AuralArc",
                "Navidrome playlist edit failed",
                e
            )

            NavidromeApiResult(
                success = false,
                message = e.message
                    ?: "The Navidrome server did not accept the change."
            )
        }
    }
}