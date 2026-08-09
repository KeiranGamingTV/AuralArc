package com.keiranhaas.auralarc.storage

import android.content.Context
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object PlaylistStore {

    private const val FILE_NAME =
        "auralarc_playlists.json"

    fun getPlaylists(
        context: Context
    ): List<Playlist> {
        return loadPlaylists(
            context
        )
    }

    fun getPlaylist(
        context: Context,
        playlistId: String
    ): Playlist? {
        return loadPlaylists(
            context
        ).firstOrNull {
            it.id == playlistId
        }
    }

    fun createPlaylistFromTrackKeys(
        context: Context,
        rawName: String,
        trackKeys: List<String>
    ): Playlist {
        val playlist =
            createPlaylist(
                context,
                rawName
            )

        val playlists =
            loadPlaylists(
                context
            ).map { existing ->
                if (
                    existing.id == playlist.id
                ) {
                    val cleanKeys =
                        trackKeys
                            .filter {
                                it.isNotBlank()
                            }
                            .distinct()

                    existing.copy(
                        trackKeys = cleanKeys,
                        trackCount = cleanKeys.size,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    existing
                }
            }

        savePlaylists(
            context,
            playlists
        )

        return getPlaylist(
            context,
            playlist.id
        ) ?: playlist
    }

    fun createPlaylist(
        context: Context,
        rawName: String
    ): Playlist {
        val name =
            rawName.trim()
                .ifBlank {
                    "New Playlist"
                }

        val playlists =
            loadPlaylists(
                context
            ).toMutableList()

        val playlist =
            Playlist(
                id = UUID.randomUUID().toString(),
                name = name,
                source = "LOCAL",
                remoteId = null,
                trackKeys = emptyList(),
                trackCount = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

        playlists.add(
            playlist
        )

        savePlaylists(
            context,
            playlists
        )

        return playlist
    }

    fun createPlaylistFromTracks(
        context: Context,
        rawName: String,
        tracks: List<MusicTrack>
    ): Playlist {
        val playlist =
            createPlaylist(
                context,
                rawName
            )

        addTracksToPlaylist(
            context = context,
            playlistId = playlist.id,
            tracks = tracks
        )

        return getPlaylist(
            context,
            playlist.id
        ) ?: playlist
    }

    fun duplicatePlaylist(
        context: Context,
        playlistId: String
    ): Playlist? {
        val playlists =
            loadPlaylists(
                context
            ).toMutableList()

        val sourcePlaylist =
            playlists.firstOrNull {
                it.id == playlistId
            } ?: return null

        val copy =
            sourcePlaylist.copy(
                id = UUID.randomUUID().toString(),
                name = "${sourcePlaylist.name} Copy",
                source = "LOCAL",
                remoteId = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

        playlists.add(
            copy
        )

        savePlaylists(
            context,
            playlists
        )

        return copy
    }

    fun renamePlaylist(
        context: Context,
        playlistId: String,
        newName: String
    ) {
        val cleanName =
            newName.trim()

        if (
            cleanName.isBlank()
        ) {
            return
        }

        val playlists =
            loadPlaylists(
                context
            ).map { playlist ->
                if (
                    playlist.id == playlistId &&
                    playlist.source == "LOCAL"
                ) {
                    playlist.copy(
                        name = cleanName,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }

        savePlaylists(
            context,
            playlists
        )
    }

    fun deletePlaylist(
        context: Context,
        playlistId: String
    ) {
        val playlists =
            loadPlaylists(
                context
            ).filterNot {
                it.id == playlistId &&
                        it.source == "LOCAL"
            }

        savePlaylists(
            context,
            playlists
        )
    }

    fun addTrackToPlaylist(
        context: Context,
        playlistId: String,
        track: MusicTrack
    ) {
        addTracksToPlaylist(
            context = context,
            playlistId = playlistId,
            tracks = listOf(
                track
            )
        )
    }

    fun addTracksToPlaylist(
        context: Context,
        playlistId: String,
        tracks: List<MusicTrack>
    ) {
        if (
            tracks.isEmpty()
        ) {
            return
        }

        val playlists =
            loadPlaylists(
                context
            ).map { playlist ->
                if (
                    playlist.id == playlistId &&
                    playlist.source == "LOCAL"
                ) {
                    val newKeys =
                        playlist.trackKeys
                            .toMutableList()

                    tracks.forEach { track ->
                        if (
                            !newKeys.contains(
                                track.uri
                            )
                        ) {
                            newKeys.add(
                                track.uri
                            )
                        }
                    }

                    playlist.copy(
                        trackKeys = newKeys,
                        trackCount = newKeys.size,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }

        savePlaylists(
            context,
            playlists
        )
    }

    fun removeTrackFromPlaylist(
        context: Context,
        playlistId: String,
        trackUri: String
    ) {
        val playlists =
            loadPlaylists(
                context
            ).map { playlist ->
                if (
                    playlist.id == playlistId &&
                    playlist.source == "LOCAL"
                ) {
                    val newKeys =
                        playlist.trackKeys.filterNot {
                            it == trackUri
                        }

                    playlist.copy(
                        trackKeys = newKeys,
                        trackCount = newKeys.size,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }

        savePlaylists(
            context,
            playlists
        )
    }

    fun moveTrackInPlaylist(
        context: Context,
        playlistId: String,
        fromIndex: Int,
        toIndex: Int
    ) {
        val playlists =
            loadPlaylists(
                context
            ).map { playlist ->
                if (
                    playlist.id == playlistId &&
                    playlist.source == "LOCAL" &&
                    fromIndex in playlist.trackKeys.indices &&
                    toIndex in playlist.trackKeys.indices
                ) {
                    val keys =
                        playlist.trackKeys.toMutableList()

                    val moved =
                        keys.removeAt(
                            fromIndex
                        )

                    keys.add(
                        toIndex,
                        moved
                    )

                    playlist.copy(
                        trackKeys = keys,
                        trackCount = keys.size,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    playlist
                }
            }

        savePlaylists(
            context,
            playlists
        )
    }

    fun getTracksForPlaylist(
        playlist: Playlist,
        allTracks: List<MusicTrack>
    ): List<MusicTrack> {
        if (
            playlist.trackKeys.isEmpty()
        ) {
            return emptyList()
        }

        return playlist.trackKeys.mapNotNull { trackKey ->
            allTracks.firstOrNull {
                it.uri == trackKey
            }
        }
    }

    private fun loadPlaylists(
        context: Context
    ): List<Playlist> {
        return try {
            val file =
                context.filesDir.resolve(
                    FILE_NAME
                )

            if (
                !file.exists()
            ) {
                return emptyList()
            }

            val rawJson =
                file.readText()

            val array =
                JSONArray(
                    rawJson
                )

            val playlists =
                mutableListOf<Playlist>()

            for (
            index in 0 until array.length()
            ) {
                val item =
                    array.optJSONObject(
                        index
                    ) ?: continue

                val keysArray =
                    item.optJSONArray(
                        "trackKeys"
                    ) ?: JSONArray()

                val keys =
                    mutableListOf<String>()

                for (
                keyIndex in 0 until keysArray.length()
                ) {
                    val key =
                        keysArray.optString(
                            keyIndex,
                            ""
                        )

                    if (
                        key.isNotBlank()
                    ) {
                        keys.add(
                            key
                        )
                    }
                }

                val playlist =
                    Playlist(
                        id = item.optString(
                            "id",
                            UUID.randomUUID().toString()
                        ),
                        name = item.optString(
                            "name",
                            "Playlist"
                        ),
                        source = item.optString(
                            "source",
                            "LOCAL"
                        ),
                        remoteId = item.optString(
                            "remoteId",
                            ""
                        ).ifBlank {
                            null
                        },
                        trackKeys = keys,
                        trackCount = item.optInt(
                            "trackCount",
                            keys.size
                        ),
                        createdAt = item.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        ),
                        updatedAt = item.optLong(
                            "updatedAt",
                            System.currentTimeMillis()
                        )
                    )

                playlists.add(
                    playlist
                )
            }

            playlists.sortedBy {
                it.name.lowercase()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists(
        context: Context,
        playlists: List<Playlist>
    ) {
        val array =
            JSONArray()

        playlists.forEach { playlist ->
            val item =
                JSONObject()

            item.put(
                "id",
                playlist.id
            )

            item.put(
                "name",
                playlist.name
            )

            item.put(
                "source",
                playlist.source
            )

            item.put(
                "remoteId",
                playlist.remoteId ?: ""
            )

            val keysArray =
                JSONArray()

            playlist.trackKeys.forEach { key ->
                keysArray.put(
                    key
                )
            }

            item.put(
                "trackKeys",
                keysArray
            )

            item.put(
                "trackCount",
                playlist.trackKeys.size
            )

            item.put(
                "createdAt",
                playlist.createdAt
            )

            item.put(
                "updatedAt",
                playlist.updatedAt
            )

            array.put(
                item
            )
        }

        context.filesDir.resolve(
            FILE_NAME
        ).writeText(
            array.toString()
        )
    }
}