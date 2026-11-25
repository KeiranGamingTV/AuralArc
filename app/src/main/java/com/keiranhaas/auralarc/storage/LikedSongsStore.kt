package com.keiranhaas.auralarc.storage

import android.content.Context
import androidx.compose.runtime.mutableLongStateOf
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.Playlist
import com.keiranhaas.auralarc.data.TrackIdentity
import org.json.JSONArray

/**
 * Stores the user's Liked Songs as stable track IDs (newest first).
 * Stable IDs work for both local files and Navidrome tracks, whose
 * stream URLs change every session.
 */
object LikedSongsStore {

    const val PLAYLIST_ID =
        "auralarc_liked_songs"

    const val PLAYLIST_NAME =
        "Liked Songs"

    private const val FILE_NAME =
        "auralarc_liked_songs.json"

    /*
     * Read this inside a composable (for example as a remember key)
     * so the UI refreshes whenever the liked list changes.
     */
    val revision =
        mutableLongStateOf(
            0L
        )

    private val lock =
        Any()

    private var cachedIds: MutableList<String>? =
        null

    private fun loadIds(
        context: Context
    ): MutableList<String> {
        synchronized(lock) {
            cachedIds?.let { cached ->
                return cached
            }

            val loaded =
                mutableListOf<String>()

            try {
                val file =
                    context.applicationContext
                        .filesDir
                        .resolve(
                            FILE_NAME
                        )

                if (
                    file.exists()
                ) {
                    val array =
                        JSONArray(
                            file.readText()
                        )

                    for (
                    index in 0 until array.length()
                    ) {
                        val id =
                            array.optString(
                                index,
                                ""
                            )

                        if (
                            id.isNotBlank() &&
                            !loaded.contains(
                                id
                            )
                        ) {
                            loaded.add(
                                id
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // A damaged file simply starts an empty list.
            }

            cachedIds =
                loaded

            return loaded
        }
    }

    private fun saveIds(
        context: Context,
        ids: List<String>
    ) {
        try {
            val array =
                JSONArray()

            ids.forEach { id ->
                array.put(
                    id
                )
            }

            context.applicationContext
                .filesDir
                .resolve(
                    FILE_NAME
                )
                .writeText(
                    array.toString()
                )
        } catch (_: Exception) {
            // Keep the in-memory list even if the write fails.
        }
    }

    fun isLiked(
        context: Context,
        track: MusicTrack
    ): Boolean {
        val stableId =
            TrackIdentity.getStableId(
                track
            )

        return synchronized(lock) {
            loadIds(
                context
            ).contains(
                stableId
            )
        }
    }

    fun like(
        context: Context,
        track: MusicTrack
    ) {
        val stableId =
            TrackIdentity.getStableId(
                track
            )

        synchronized(lock) {
            val ids =
                loadIds(
                    context
                )

            if (
                !ids.contains(
                    stableId
                )
            ) {
                ids.add(
                    0,
                    stableId
                )

                saveIds(
                    context,
                    ids
                )

                revision.value += 1L
            }
        }
    }

    fun unlike(
        context: Context,
        track: MusicTrack
    ) {
        val stableId =
            TrackIdentity.getStableId(
                track
            )

        synchronized(lock) {
            val ids =
                loadIds(
                    context
                )

            if (
                ids.remove(
                    stableId
                )
            ) {
                saveIds(
                    context,
                    ids
                )

                revision.value += 1L
            }
        }
    }

        /**
     * Adds tracks to Liked Songs (for example from an imported M3U).
     * Songs that are already liked stay where they are. New songs go
     * to the top in the same order they were given. Returns how many
     * songs were newly added.
     */
    fun addTracks(
        context: Context,
        tracks: List<MusicTrack>
    ): Int {
        synchronized(lock) {
            val ids =
                loadIds(
                    context
                )

            val existingIds =
                ids.toHashSet()

            val newIds =
                LinkedHashSet<String>()

            tracks.forEach { track ->
                val stableId =
                    TrackIdentity.getStableId(
                        track
                    )

                if (
                    stableId !in existingIds
                ) {
                    newIds.add(
                        stableId
                    )
                }
            }

            if (
                newIds.isEmpty()
            ) {
                return 0
            }

            ids.addAll(
                0,
                newIds.toList()
            )

            saveIds(
                context,
                ids
            )

            revision.value += 1L

            return newIds.size
        }
    }

    /** True for playlist names like "Liked Songs" or "liked_songs". */
    fun isLikedSongsName(
        rawName: String
    ): Boolean {
        val normalized =
            rawName
                .replace(
                    Regex(
                        "[_\\-]+"
                    ),
                    " "
                )
                .replace(
                    Regex(
                        "\\s+"
                    ),
                    " "
                )
                .trim()

        return normalized.equals(
            PLAYLIST_NAME,
            ignoreCase = true
        )
    }

    /** Returns true when the track is liked after the toggle. */
    fun toggle(
        context: Context,
        track: MusicTrack
    ): Boolean {
        return if (
            isLiked(
                context,
                track
            )
        ) {
            unlike(
                context,
                track
            )

            false
        } else {
            like(
                context,
                track
            )

            true
        }
    }

    /** Liked tracks that exist in [allTracks], most recently liked first. */
    fun getLikedTracks(
        context: Context,
        allTracks: List<MusicTrack>
    ): List<MusicTrack> {
        val ids =
            synchronized(lock) {
                loadIds(
                    context
                ).toList()
            }

        if (
            ids.isEmpty()
        ) {
            return emptyList()
        }

        val tracksById =
            HashMap<String, MusicTrack>(
                allTracks.size
            )

        allTracks.forEach { track ->
            tracksById.putIfAbsent(
                TrackIdentity.getStableId(
                    track
                ),
                track
            )
        }

        return ids.mapNotNull { id ->
            tracksById[id]
        }
    }

    /** The virtual playlist object used by the playlist screens. */
    fun asPlaylist(
        trackCount: Int
    ): Playlist {
        return Playlist(
            id = PLAYLIST_ID,
            name = PLAYLIST_NAME,
            source = "LOCAL",
            remoteId = null,
            trackKeys = emptyList(),
            trackCount = trackCount,
            createdAt = 0L,
            updatedAt = 0L
        )
    }
}