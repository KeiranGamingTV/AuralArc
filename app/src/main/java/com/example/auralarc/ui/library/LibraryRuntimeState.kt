package com.example.auralarc.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.example.auralarc.data.LibrarySource
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.storage.NavigationPreferences

object LibraryRuntimeState {

    val rootTab =
        mutableStateOf(
            LibraryRootTab.HOME
        )

    val libraryMode =
        mutableStateOf(
            LibraryMode.ALBUMS
        )

    val localTracks =
        mutableStateOf<List<MusicTrack>?>(
            null
        )

    val navidromeTracks =
        mutableStateOf<List<MusicTrack>?>(
            null
        )

    val isLibraryLoading =
        mutableStateOf(
            false
        )

    val navidromeFailedToLoad =
        mutableStateOf(
            false
        )

    private var localRefreshCompleted =
        false

    private var navidromeRefreshCompleted =
        false

    fun restoreNavigationState(
        context: Context
    ) {
        rootTab.value =
            NavigationPreferences.getLastRootTab(
                context
            )

        libraryMode.value =
            NavigationPreferences.getLastLibraryMode(
                context
            )
    }

    fun getTracks(
        source: LibrarySource
    ): List<MusicTrack> {
        return when (
            source
        ) {
            LibrarySource.LOCAL ->
                localTracks.value
                    ?: emptyList()

            LibrarySource.NAVIDROME ->
                navidromeTracks.value
                    ?: emptyList()
        }
    }

    fun hasCache(
        source: LibrarySource
    ): Boolean {
        return when (
            source
        ) {
            LibrarySource.LOCAL ->
                localTracks.value != null

            LibrarySource.NAVIDROME ->
                navidromeTracks.value != null
        }
    }

    fun hasCompletedRefresh(
        source: LibrarySource
    ): Boolean {
        return when (
            source
        ) {
            LibrarySource.LOCAL ->
                localRefreshCompleted

            LibrarySource.NAVIDROME ->
                navidromeRefreshCompleted
        }
    }

    fun markRefreshCompleted(
        source: LibrarySource
    ) {
        when (
            source
        ) {
            LibrarySource.LOCAL ->
                localRefreshCompleted =
                    true

            LibrarySource.NAVIDROME ->
                navidromeRefreshCompleted =
                    true
        }
    }

    fun setTracks(
        source: LibrarySource,
        tracks: List<MusicTrack>
    ) {
        when (
            source
        ) {
            LibrarySource.LOCAL ->
                localTracks.value =
                    tracks

            LibrarySource.NAVIDROME ->
                navidromeTracks.value =
                    tracks
        }
    }

    fun replaceTracks(
        updatedTracks: List<MusicTrack>
    ) {
        if (
            updatedTracks.isEmpty()
        ) {
            return
        }

        val replacements =
            updatedTracks.associateBy { track ->
                track.uri
            }

        localTracks.value =
            localTracks.value?.map { existingTrack ->
                replacements[existingTrack.uri]
                    ?: existingTrack
            }

        navidromeTracks.value =
            navidromeTracks.value?.map { existingTrack ->
                replacements[existingTrack.uri]
                    ?: existingTrack
            }
    }

    fun clearCache(
        source: LibrarySource
    ) {
        when (
            source
        ) {
            LibrarySource.LOCAL -> {
                localTracks.value =
                    null

                localRefreshCompleted =
                    false
            }

            LibrarySource.NAVIDROME -> {
                navidromeTracks.value =
                    null

                navidromeRefreshCompleted =
                    false
            }
        }
    }
}