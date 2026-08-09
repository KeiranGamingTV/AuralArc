package com.keiranhaas.auralarc.storage

import android.content.Context
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.data.TrackIdentity
import java.time.LocalDate

object TodaysPicksPreferences {

    private const val PREFS_NAME =
        "todays_picks_prefs"

    private const val KEY_ENABLED =
        "todays_picks_enabled"

    private const val KEY_PICK_DATE =
        "todays_picks_date"

    private const val KEY_PICK_TRACK_IDS =
        "todays_picks_track_ids"

    fun getEnabled(
        context: Context
    ): Boolean {
        return context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                KEY_ENABLED,
                true
            )
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }

    fun getOrCreateTodaysPicks(
        context: Context,
        availableTracks: List<MusicTrack>,
        count: Int = 30
    ): List<MusicTrack> {

        val eligibleTracks =
            availableTracks.filter { track ->
                track.uri.isNotBlank()
            }

        if (
            eligibleTracks.isEmpty()
        ) {
            return emptyList()
        }

        val applicationContext =
            context.applicationContext

        val preferences =
            applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        val today =
            LocalDate.now()
                .toString()

        val savedDate =
            preferences.getString(
                KEY_PICK_DATE,
                null
            )

        val savedTrackIds =
            preferences
                .getString(
                    KEY_PICK_TRACK_IDS,
                    ""
                )
                .orEmpty()
                .lineSequence()
                .map { id ->
                    id.trim()
                }
                .filter { id ->
                    id.isNotBlank()
                }
                .toList()

        val tracksByStableId =
            eligibleTracks.associateBy { track ->
                TrackIdentity.getStableId(
                    track
                )
            }

        /*
         * If today's selection already exists,
         * reconstruct it in exactly the same order.
         */
        if (
            savedDate == today &&
            savedTrackIds.isNotEmpty()
        ) {
            val restoredTracks =
                savedTrackIds
                    .mapNotNull { stableId ->
                        tracksByStableId[
                            stableId
                        ]
                    }
                    .toMutableList()

            /*
             * If a track disappeared from the server
             * during the day, keep every surviving pick
             * in the same order and only fill the gaps.
             */
            val desiredCount =
                minOf(
                    count,
                    eligibleTracks.size
                )

            if (
                restoredTracks.size <
                desiredCount
            ) {
                val restoredIds =
                    restoredTracks
                        .map { track ->
                            TrackIdentity.getStableId(
                                track
                            )
                        }
                        .toHashSet()

                val replacements =
                    eligibleTracks
                        .filter { track ->
                            TrackIdentity.getStableId(
                                track
                            ) !in restoredIds
                        }
                        .shuffled()
                        .take(
                            desiredCount -
                                    restoredTracks.size
                        )

                restoredTracks.addAll(
                    replacements
                )

                saveTodaysPicks(
                    context = applicationContext,
                    date = today,
                    tracks = restoredTracks
                )
            }

            if (
                restoredTracks.isNotEmpty()
            ) {
                return restoredTracks.take(
                    desiredCount
                )
            }
        }

        /*
         * No valid selection exists for today.
         * Generate one and persist the exact order.
         */
        val newPicks =
            eligibleTracks
                .shuffled()
                .take(
                    minOf(
                        count,
                        eligibleTracks.size
                    )
                )

        saveTodaysPicks(
            context = applicationContext,
            date = today,
            tracks = newPicks
        )

        return newPicks
    }

    private fun saveTodaysPicks(
        context: Context,
        date: String,
        tracks: List<MusicTrack>
    ) {
        val stableIds =
            tracks.joinToString(
                separator = "\n"
            ) { track ->
                TrackIdentity.getStableId(
                    track
                )
            }

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_PICK_DATE,
                date
            )
            .putString(
                KEY_PICK_TRACK_IDS,
                stableIds
            )
            .apply()
    }

    fun clearSavedPicks(
        context: Context
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_PICK_DATE
            )
            .remove(
                KEY_PICK_TRACK_IDS
            )
            .apply()
    }
}