package com.keiranhaas.auralarc.ui

import android.content.Context
import com.keiranhaas.auralarc.data.LibrarySource
import com.keiranhaas.auralarc.data.MusicTrack
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.storage.LibraryCacheStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun applyUpdatedAudioTracks(
    context: Context,
    updatedTracks: List<MusicTrack>
) {
    if (
        updatedTracks.isEmpty()
    ) {
        return
    }

    val localLibrarySnapshot =
        withContext(
            Dispatchers.Main.immediate
        ) {
            LibraryRuntimeState.replaceTracks(
                updatedTracks
            )

            PlayerManager.applyUpdatedTrackMetadata(
                updatedTracks
            )

            val selectedTrack =
                TrackInfoNavigationState
                    .selectedTrack
                    .value

            if (
                selectedTrack != null
            ) {
                val replacement =
                    updatedTracks.firstOrNull { track ->
                        track.uri ==
                                selectedTrack.uri
                    }

                if (
                    replacement != null
                ) {
                    TrackInfoNavigationState
                        .selectedTrack
                        .value =
                        replacement
                }
            }

            LibraryRuntimeState
                .localTracks
                .value
                .orEmpty()
        }

    withContext(
        Dispatchers.IO
    ) {
        LibraryCacheStore.saveTracks(
            context =
            context.applicationContext,
            source =
            LibrarySource.LOCAL,
            tracks =
            localLibrarySnapshot
        )
    }
}