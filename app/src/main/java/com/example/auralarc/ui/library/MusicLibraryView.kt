package com.example.auralarc.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.auralarc.data.LibrarySource
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.navidrome.NavidromeLibraryManager
import com.example.auralarc.navigation.Screen
import com.example.auralarc.player.PlaybackState
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.player.QueueManager
import com.example.auralarc.scanner.LibraryManager
import com.example.auralarc.storage.LibrarySourcePreferences
import com.example.auralarc.storage.LibraryCacheStore
import com.example.auralarc.storage.ListeningStatsStore
import com.example.auralarc.storage.TrackListeningStats
import com.example.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.auralarc.storage.TodaysPicksPreferences
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.auralarc.ui.theme.AuralArcCrossfade
import com.example.auralarc.ui.theme.AuralArcMotion
import android.app.Activity
import android.app.PendingIntent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.auralarc.storage.AudioTagEditResult
import com.example.auralarc.storage.AudioTagEditor
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.auralarc.storage.NavigationPreferences

private fun loadTracksSafely(
    context: Context
): List<MusicTrack> {
    return try {
        LibraryManager.loadLibrary(
            context
        )
    } catch (e: Exception) {
        Log.e(
            "AuralArc",
            "Library load crashed",
            e
        )

        emptyList()
    }
}

private fun loadTracksForSource(
    context: Context,
    source: LibrarySource
): List<MusicTrack>? {
    return when (
        source
    ) {
        LibrarySource.LOCAL ->
            loadTracksSafely(
                context
            )

        LibrarySource.NAVIDROME ->
            NavidromeLibraryManager.loadLibraryOrNull(
                context
            )
    }
}

private fun trackMatchesSearch(
    track: MusicTrack,
    query: String
): Boolean {
    if (
        query.isBlank()
    ) {
        return true
    }

    val cleanQuery =
        query.trim().lowercase()

    return track.title.lowercase().contains(
        cleanQuery
    ) ||
            track.artist.lowercase().contains(
                cleanQuery
            ) ||
            track.album.lowercase().contains(
                cleanQuery
            )
}

private fun sortTracks(
    tracks: List<MusicTrack>,
    sortMode: TrackSortMode,
    descending: Boolean
): List<MusicTrack> {
    if (
        sortMode == TrackSortMode.RELEASE_DATE
    ) {
        return if (
            descending
        ) {
            tracks.sortedWith(
                compareByDescending<MusicTrack> {
                    if (
                        it.releaseYear > 0
                    ) {
                        it.releaseYear
                    } else {
                        Int.MIN_VALUE
                    }
                }.thenBy {
                    it.album.lowercase()
                }.thenBy {
                    safeTrackNumber(
                        it
                    )
                }.thenBy {
                    it.title.lowercase()
                }
            )
        } else {
            tracks.sortedWith(
                compareBy<MusicTrack> {
                    if (
                        it.releaseYear > 0
                    ) {
                        it.releaseYear
                    } else {
                        Int.MAX_VALUE
                    }
                }.thenBy {
                    it.album.lowercase()
                }.thenBy {
                    safeTrackNumber(
                        it
                    )
                }.thenBy {
                    it.title.lowercase()
                }
            )
        }
    }

    val sortedTracks =
        when (
            sortMode
        ) {
            TrackSortMode.TITLE ->
                tracks.sortedBy {
                    it.title.lowercase()
                }

            TrackSortMode.ARTIST ->
                tracks.sortedBy {
                    it.artist.lowercase()
                }

            TrackSortMode.ALBUM ->
                tracks.sortedBy {
                    it.album.lowercase()
                }

            TrackSortMode.RELEASE_DATE ->
                tracks
        }

    return if (
        descending
    ) {
        sortedTracks.reversed()
    } else {
        sortedTracks
    }
}

private fun safeTrackNumber(
    track: MusicTrack
): Int {
    return if (
        track.trackNumber > 0
    ) {
        track.trackNumber
    } else {
        Int.MAX_VALUE
    }
}

private fun sortAlbumSongs(
    tracks: List<MusicTrack>
): List<MusicTrack> {
    return tracks.sortedWith(
        compareBy<MusicTrack> {
            if (
                it.trackNumber > 0
            ) {
                it.trackNumber
            } else {
                Int.MAX_VALUE
            }
        }.thenBy {
            it.title.lowercase()
        }
    )
}

private fun playTrackFromList(
    context: Context,
    track: MusicTrack,
    queueTracks: List<MusicTrack>
) {
    QueueManager.setQueue(
        queueTracks,
        track
    )

    PlayerManager.playTrack(
        context = context,
        track = track,
        queueTracks = queueTracks
    )

    PlayerManager.applyPlaybackModes()
}

private fun playTracks(
    context: Context,
    tracks: List<MusicTrack>
) {
    if (
        tracks.isEmpty()
    ) {
        return
    }

    val firstTrack =
        tracks.first()

    PlaybackState.shuffleEnabled.value =
        false

    QueueManager.setQueuePreservingOrder(
        tracks,
        firstTrack
    )

    PlayerManager.playTrack(
        context = context,
        track = firstTrack,
        queueTracks = tracks
    )
}

private fun shufflePlayTracks(
    context: Context,
    tracks: List<MusicTrack>
) {
    if (
        tracks.isEmpty()
    ) {
        return
    }

    val firstTrack =
        tracks.random()

    PlaybackState.shuffleEnabled.value =
        true

    QueueManager.setQueue(
        tracks,
        firstTrack
    )

    PlayerManager.playTrack(
        context = context,
        track = firstTrack,
        queueTracks = tracks
    )

    PlayerManager.applyPlaybackModes()
}

private fun totalDurationText(
    tracks: List<MusicTrack>
): String {
    val totalMillis =
        tracks.map {
            it.duration
        }.sum()

    val totalSeconds =
        totalMillis / 1000

    val hours =
        totalSeconds / 3600

    val minutes =
        (totalSeconds % 3600) / 60

    return if (
        hours > 0
    ) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun albumArtistText(
    tracks: List<MusicTrack>
): String {
    val joined =
        tracks
            .map {
                it.albumArtist
            }
            .distinct()
            .joinToString(
                ", "
            )

    return if (
        joined.isBlank()
    ) {
        "Unknown Artist"
    } else {
        joined
    }
}

private fun metadataKey(
    title: String,
    artist: String,
    album: String
): String {
    return listOf(
        title,
        artist,
        album
    ).joinToString(
        "|"
    ) {
        it.trim()
            .lowercase()
            .replace(
                Regex("""\s+"""),
                " "
            )
    }
}

private fun statForTrack(
    track: MusicTrack,
    statsByUri: Map<String, TrackListeningStats>,
    statsByMetadata: Map<String, TrackListeningStats>
): TrackListeningStats? {
    return statsByUri[track.uri]
        ?: statsByMetadata[
            metadataKey(
                track.title,
                track.artist,
                track.album
            )
        ]
}

private fun todaysLeastListenedPicks(
    navidromeTracks: List<MusicTrack>,
    allStats: List<TrackListeningStats>
): List<MusicTrack> {
    if (
        navidromeTracks.isEmpty()
    ) {
        return emptyList()
    }

    val statsByUri =
        allStats.associateBy {
            it.trackUri
        }

    val statsByMetadata =
        allStats.associateBy {
            metadataKey(
                it.title,
                it.artist,
                it.album
            )
        }

    val rankedTracks =
        navidromeTracks.map { track ->
            val stat =
                statForTrack(
                    track = track,
                    statsByUri = statsByUri,
                    statsByMetadata = statsByMetadata
                )

            Triple(
                track,
                stat?.playCount ?: 0,
                stat?.listeningMillis ?: 0L
            )
        }.sortedWith(
            compareBy<Triple<MusicTrack, Int, Long>> {
                it.second
            }.thenBy {
                it.third
            }
        )

    val todaySeed =
        System.currentTimeMillis() / 86_400_000L

    val random =
        Random(
            todaySeed
        )

    val picks =
        mutableListOf<MusicTrack>()

    var index =
        0

    while (
        picks.size < 30 &&
        index < rankedTracks.size
    ) {
        val playCount =
            rankedTracks[index].second

        val listeningMillis =
            rankedTracks[index].third

        val group =
            rankedTracks
                .drop(
                    index
                )
                .takeWhile {
                    it.second == playCount &&
                            it.third == listeningMillis
                }

        picks.addAll(
            group
                .map {
                    it.first
                }
                .shuffled(
                    random
                )
                .take(
                    30 - picks.size
                )
        )

        index +=
            group.size
    }

    return picks
}

private data class ArtistAlbumGroup(
    val albumName: String,
    val tracks: List<MusicTrack>,
    val releaseYear: Int,
    val albumArtPath: String?
)

private fun artistAlbumGroups(
    tracks: List<MusicTrack>
): List<ArtistAlbumGroup> {
    return tracks
        .groupBy {
            it.album
        }
        .map { entry ->
            val albumTracks =
                sortAlbumSongs(
                    entry.value
                )

            val releaseYear =
                albumTracks
                    .map {
                        it.releaseYear
                    }
                    .filter {
                        it > 0
                    }
                    .minOrNull()
                    ?: 0

            val albumArtPath =
                albumTracks
                    .firstOrNull {
                        !it.albumArtPath.isNullOrBlank()
                    }
                    ?.albumArtPath

            ArtistAlbumGroup(
                albumName = entry.key,
                tracks = albumTracks,
                releaseYear = releaseYear,
                albumArtPath = albumArtPath
            )
        }
        .sortedWith(
            compareBy<ArtistAlbumGroup> {
                if (
                    it.releaseYear > 0
                ) {
                    it.releaseYear
                } else {
                    Int.MAX_VALUE
                }
            }.thenBy {
                it.albumName.lowercase()
            }
        )
}

private fun sortArtistSongsForPlayAll(
    tracks: List<MusicTrack>
): List<MusicTrack> {
    return tracks.sortedWith(
        compareBy<MusicTrack> {
            if (
                it.releaseYear > 0
            ) {
                it.releaseYear
            } else {
                Int.MAX_VALUE
            }
        }.thenBy {
            it.album.lowercase()
        }.thenBy {
            safeTrackNumber(
                it
            )
        }.thenBy {
            it.title.lowercase()
        }
    )
}

@Composable
fun MusicLibraryView(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var rootTab by LibraryRuntimeState.rootTab

    var librarySource by remember {
        mutableStateOf(
            LibrarySourcePreferences.getLibrarySource(
                context
            )
        )
    }

    val tracks =
        LibraryRuntimeState.getTracks(
            librarySource
        )

    val localTracks =
        LibraryRuntimeState.getTracks(
            LibrarySource.LOCAL
        )

    val navidromeTracks =
        LibraryRuntimeState.getTracks(
            LibrarySource.NAVIDROME
        )

    val isLibraryLoading =
        LibraryRuntimeState.isLibraryLoading.value

    var searchQuery by rememberSaveable {
        mutableStateOf(
            ""
        )
    }

    var sortMode by remember {
        mutableStateOf(
            TrackSortMode.TITLE
        )
    }

    var sortDescending by remember {
        mutableStateOf(false)
    }

    var libraryMode by LibraryRuntimeState.libraryMode

    var selectedAlbum by rememberSaveable {
        mutableStateOf<String?>(
            null
        )
    }

    var selectedArtist by rememberSaveable {
        mutableStateOf<String?>(
            null
        )
    }

    var selectedPlaylistId by rememberSaveable {
        mutableStateOf<String?>(
            null
        )
    }

    fun selectRootTab(
        newTab: LibraryRootTab
    ) {
        rootTab =
            newTab

        NavigationPreferences.setLastRootTab(
            context = context,
            tab = newTab
        )
    }

    fun selectLibraryMode(
        newMode: LibraryMode
    ) {
        libraryMode =
            newMode

        NavigationPreferences.setLastLibraryMode(
            context = context,
            mode = newMode
        )
    }

    var filterDialogVisible by remember {
        mutableStateOf(false)
    }

    var todaysPicksEnabled by remember {
        mutableStateOf(
            TodaysPicksPreferences.getEnabled(
                context
            )
        )
    }

    suspend fun restoreCachedLibrary(
        source: LibrarySource
    ) {
        if (
            LibraryRuntimeState.hasCache(
                source
            )
        ) {
            return
        }

        val cachedTracks =
            withContext(
                Dispatchers.IO
            ) {
                LibraryCacheStore.loadTracks(
                    context = context.applicationContext,
                    source = source
                )
            }

        if (
            cachedTracks != null
        ) {
            LibraryRuntimeState.setTracks(
                source = source,
                tracks = cachedTracks
            )
        }
    }

    suspend fun refreshLibrary(
        resetSelection: Boolean,
        source: LibrarySource,
        forceRefresh: Boolean = false
    ) {
        if (
            !forceRefresh &&
            LibraryRuntimeState.hasCompletedRefresh(
                source
            )
        ) {
            return
        }

        LibraryRuntimeState.isLibraryLoading.value =
            true

        LibraryRuntimeState.navidromeFailedToLoad.value =
            false

        val loadedTracks =
            if (
                source == LibrarySource.NAVIDROME
            ) {
                try {
                    withTimeoutOrNull(
                        45_000L
                    ) {
                        withContext(
                            Dispatchers.IO
                        ) {
                            loadTracksForSource(
                                context = context.applicationContext,
                                source = source
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "AuralArc",
                        "Navidrome load crashed",
                        e
                    )

                    null
                }
            } else {
                withContext(
                    Dispatchers.IO
                ) {
                    loadTracksForSource(
                        context = context.applicationContext,
                        source = source
                    )
                }
            }

        if (
            loadedTracks == null
        ) {
            LibraryRuntimeState.isLibraryLoading.value =
                false

            if (
                source == LibrarySource.NAVIDROME
            ) {
                LibraryRuntimeState.navidromeFailedToLoad.value =
                    true
            }

            return
        }

        LibraryRuntimeState.setTracks(
            source = source,
            tracks = loadedTracks
        )

        withContext(
            Dispatchers.IO
        ) {
            LibraryCacheStore.saveTracks(
                context = context.applicationContext,
                source = source,
                tracks = loadedTracks
            )
        }

        LibraryRuntimeState.markRefreshCompleted(
            source
        )

        if (
            resetSelection
        ) {
            selectedAlbum =
                null

            selectedArtist =
                null

            selectedPlaylistId =
                null
        }

        LibraryRuntimeState.isLibraryLoading.value =
            false
    }

    val searchedTracks =
        sortTracks(
            tracks =
            tracks.filter { track ->
                trackMatchesSearch(
                    track,
                    searchQuery
                )
            },
            sortMode = sortMode,
            descending = sortDescending
        )

    LaunchedEffect(
        librarySource
    ) {
        restoreCachedLibrary(
            librarySource
        )

        refreshLibrary(
            resetSelection = false,
            source = librarySource,
            forceRefresh = false
        )
    }

    LaunchedEffect(
        librarySource,
        tracks.size
    ) {
        if (
            tracks.isNotEmpty() &&
            PlayerManager.canRestoreLastSession()
        ) {
            PlayerManager.restoreLastSession(
                context,
                tracks
            )
        }
    }

    BackHandler(
        enabled =
        searchQuery.isNotBlank() ||
                selectedPlaylistId != null ||
                selectedAlbum != null ||
                selectedArtist != null ||
                filterDialogVisible
    ) {
        when {
            filterDialogVisible -> {
                filterDialogVisible =
                    false
            }

            searchQuery.isNotBlank() -> {
                searchQuery =
                    ""
            }

            selectedPlaylistId != null -> {
                selectedPlaylistId =
                    null
            }

            selectedAlbum != null -> {
                selectedAlbum =
                    null
            }

            selectedArtist != null -> {
                selectedArtist =
                    null
            }
        }
    }

    Scaffold(
        backgroundColor = AuralArcStyle.BackgroundBottom,
        topBar = {
            TopAppBar(
                backgroundColor = AuralArcStyle.BackgroundTop,
                elevation = 0.dp,
                title = {
                    Text(
                        text =
                        when (
                            rootTab
                        ) {
                            LibraryRootTab.HOME ->
                                "Home"

                            LibraryRootTab.SEARCH ->
                                "Search"

                            LibraryRootTab.LIBRARY ->
                                "Library"
                        },
                        color = AuralArcStyle.TextPrimary
                    )
                },
                actions = {
                    AuralArcIconButton(
                        enabled = !isLibraryLoading,
                        onClick = {
                            scope.launch {
                                val sourceToRefresh =
                                    if (
                                        rootTab == LibraryRootTab.HOME
                                    ) {
                                        LibrarySource.NAVIDROME
                                    } else {
                                        librarySource
                                    }

                                LibraryRuntimeState.clearCache(
                                    sourceToRefresh
                                )

                                refreshLibrary(
                                    resetSelection = rootTab != LibraryRootTab.HOME,
                                    source = sourceToRefresh,
                                    forceRefresh = true
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint =
                            if (
                                isLibraryLoading
                            ) {
                                AuralArcStyle.TextMuted
                            } else {
                                AuralArcStyle.TextPrimary
                            },
                            modifier = Modifier.size(
                                24.dp
                            )
                        )
                    }

                    AuralArcIconButton(
                        onClick = {
                            navController.navigate(
                                Screen.ListeningStats.route
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Listening stats",
                            tint = AuralArcStyle.TextPrimary,
                            modifier = Modifier.size(
                                24.dp
                            )
                        )
                    }

                    AuralArcIconButton(
                        onClick = {
                            navController.navigate(
                                Screen.Settings.route
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AuralArcStyle.TextPrimary,
                            modifier = Modifier.size(
                                24.dp
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            MainBottomBar(
                navController = navController,
                selectedTab = rootTab,
                onTabSelected = { newTab ->
                    selectRootTab(
                        newTab
                    )

                    if (
                        newTab != LibraryRootTab.SEARCH
                    ) {
                        searchQuery =
                            ""
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .background(
                    brush = AuralArcStyle.appBackgroundBrush()
                )
        ) {
            AuralArcCrossfade(
                targetState = rootTab
            ) { activeRootTab ->
                when (
                    activeRootTab
                ) {
                    LibraryRootTab.HOME -> {
                        HomeTabContent(
                            context = context,
                            navidromeTracks = navidromeTracks,
                            localTracks = localTracks,
                            librarySource = librarySource,
                            isLibraryLoading = isLibraryLoading,
                            todaysPicksEnabled = todaysPicksEnabled,
                            onRetryNavidrome = {
                                scope.launch {
                                    LibraryRuntimeState.clearCache(
                                        LibrarySource.NAVIDROME
                                    )

                                    refreshLibrary(
                                        resetSelection = false,
                                        source = LibrarySource.NAVIDROME,
                                        forceRefresh = true
                                    )
                                }
                            }
                        )
                    }

                    LibraryRootTab.SEARCH -> {
                        SearchTabContent(
                            navController = navController,
                            selectedSource = librarySource,
                            isLibraryLoading = isLibraryLoading,
                            onSourceSelected = { newSource ->
                                if (
                                    newSource != librarySource
                                ) {
                                    librarySource =
                                        newSource

                                    LibrarySourcePreferences.saveLibrarySource(
                                        context,
                                        newSource
                                    )
                                }
                            },
                            searchQuery = searchQuery,
                            onSearchQueryChange = {
                                searchQuery =
                                    it
                            },
                            searchedTracks = searchedTracks
                        )
                    }

                    LibraryRootTab.LIBRARY -> {
                        LibraryTabContent(
                            context = context,
                            navController = navController,
                            selectedSource = librarySource,
                            isLibraryLoading = isLibraryLoading,
                            onSourceSelected = { newSource ->
                                if (
                                    newSource != librarySource
                                ) {
                                    librarySource =
                                        newSource

                                    LibrarySourcePreferences.saveLibrarySource(
                                        context,
                                        newSource
                                    )
                                }
                            },
                            tracks = tracks,
                            searchedTracks = searchedTracks,
                            libraryMode = libraryMode,
                            onLibraryModeChange = { newMode ->
                                selectLibraryMode(
                                    newMode
                                )

                                selectedAlbum =
                                    null

                                selectedArtist =
                                    null

                                selectedPlaylistId =
                                    null
                            },
                            onOpenFilter = {
                                filterDialogVisible =
                                    true
                            },
                            selectedAlbum = selectedAlbum,
                            onSelectedAlbumChange = {
                                selectedAlbum =
                                    it
                            },
                            selectedArtist = selectedArtist,
                            onSelectedArtistChange = {
                                selectedArtist =
                                    it
                            },
                            selectedPlaylistId = selectedPlaylistId,
                            onSelectedPlaylistChange = {
                                selectedPlaylistId =
                                    it
                            },
                            librarySource = librarySource
                        )
                    }
                }
            }

            if (
                isLibraryLoading
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            3.dp
                        )
                        .align(
                            Alignment.TopCenter
                        ),
                    color = AuralArcStyle.PurpleBright,
                    backgroundColor = AuralArcStyle.Divider
                )
            }

            if (
                filterDialogVisible
            ) {
                LibraryFilterDialog(
                    sortMode = sortMode,
                    onSortModeChange = { newSortMode ->
                        sortMode =
                            newSortMode
                    },
                    sortDescending = sortDescending,
                    onSortDescendingChange = { descending ->
                        sortDescending =
                            descending
                    },
                    onDismiss = {
                        filterDialogVisible =
                            false
                    }
                )
            }

            if (
                LibraryRuntimeState.navidromeFailedToLoad.value
            ) {
                AlertDialog(
                    onDismissRequest = {
                        LibraryRuntimeState.navidromeFailedToLoad.value =
                            false
                    },
                    title = {
                        Text(
                            text = "Navidrome failed to load"
                        )
                    },
                    text = {
                        Text(
                            text = "AuralArc could not load your Navidrome library. Your connection may be slow or unavailable."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                LibraryRuntimeState.navidromeFailedToLoad.value =
                                    false

                                scope.launch {
                                    LibraryRuntimeState.clearCache(
                                        LibrarySource.NAVIDROME
                                    )

                                    refreshLibrary(
                                        resetSelection = false,
                                        source = LibrarySource.NAVIDROME,
                                        forceRefresh = true
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = "Load Today's Picks"
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                LibraryRuntimeState.navidromeFailedToLoad.value =
                                    false

                                librarySource =
                                    LibrarySource.LOCAL

                                LibrarySourcePreferences.saveLibrarySource(
                                    context,
                                    LibrarySource.LOCAL
                                )

                                selectRootTab(
                                    LibraryRootTab.LIBRARY
                                )
                            }
                        ) {
                            Text(
                                text = "Switch to Local"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ArtistAlbumsForArtistScreen(
    artistTracks: List<MusicTrack>,
    onAlbumSelected: (String) -> Unit
) {
    val albums =
        artistAlbumGroups(
            artistTracks
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 6.dp,
            bottom = 12.dp
        )
    ) {
        items(
            items = albums,
            key = { album ->
                album.albumName
            }
        ) { album ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    )
                    .auralArcClickable {
                        onAlbumSelected(
                            album.albumName
                        )
                    },
                shape = AuralArcStyle.CardShape,
                backgroundColor = AuralArcStyle.Surface,
                elevation = 7.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtwork(
                        albumArtPath = album.albumArtPath,
                        size = 72.dp
                    )

                    Spacer(
                        modifier = Modifier.width(
                            14.dp
                        )
                    )

                    Column(
                        modifier = Modifier.weight(
                            1f
                        )
                    ) {
                        Text(
                            text = album.albumName,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AuralArcStyle.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                            if (
                                album.releaseYear > 0
                            ) {
                                album.releaseYear.toString()
                            } else {
                                "Unknown release date"
                            },
                            style = MaterialTheme.typography.body2,
                            color = AuralArcStyle.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                            if (
                                album.tracks.size == 1
                            ) {
                                "1 song"
                            } else {
                                "${album.tracks.size} songs"
                            },
                            style = MaterialTheme.typography.caption,
                            color = AuralArcStyle.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    context: Context,
    navidromeTracks: List<MusicTrack>,
    localTracks: List<MusicTrack>,
    librarySource: LibrarySource,
    isLibraryLoading: Boolean,
    todaysPicksEnabled: Boolean,
    onRetryNavidrome: () -> Unit
) {

    val todaysPicks =
        remember(
            navidromeTracks
        ) {
            navidromeTracks
                .filter {
                    it.uri.isNotBlank()
                }
                .shuffled()
                .take(
                    30
                )
        }

    val homeTracks =
        when (
            librarySource
        ) {
            LibrarySource.LOCAL ->
                localTracks

            LibrarySource.NAVIDROME ->
                navidromeTracks
        }

    val recentTracks =
        remember(
            context,
            homeTracks
        ) {
            ListeningStatsStore.getRecentlyListenedTracks(
                context = context,
                allTracks = homeTracks,
                limit = 20
            )
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            )
    ) {
        item {
            if (
                todaysPicksEnabled
            ) {
                HomeSectionHeader(
                    title = "Today's Picks",
                    subtitle = "30 randomly selected least-listened Navidrome songs"
                )

                if (
                    navidromeTracks.isEmpty()
                ) {
                    AuralArcMessageCard(
                        title =
                        if (
                            isLibraryLoading
                        ) {
                            "Loading Navidrome"
                        } else {
                            "No Navidrome Songs Loaded"
                        },
                        message =
                        if (
                            isLibraryLoading
                        ) {
                            "AuralArc is loading your Navidrome library."
                        } else {
                            "Load your Navidrome library to generate Today's Picks."
                        },
                        actionText =
                        if (
                            isLibraryLoading
                        ) {
                            null
                        } else {
                            "Load Today's Picks"
                        },
                        onAction =
                        if (
                            isLibraryLoading
                        ) {
                            null
                        } else {
                            onRetryNavidrome
                        }
                    )
                } else {
                    HorizontalTwoRowTrackGrid(
                        tracks = todaysPicks,
                        onTrackSelected = { track ->
                            playTrackFromList(
                                context = context,
                                track = track,
                                queueTracks = todaysPicks
                            )
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.height(
                        18.dp
                    )
                )
            }

            HomeSectionHeader(
                title = "Recently Listened",
                subtitle = "Songs you played most recently"
            )

            if (
                recentTracks.isEmpty()
            ) {
                AuralArcMessageCard(
                    title = "No Recent Listening Yet",
                    message = "Songs will appear here after you listen to them."
                )
            } else {
                HorizontalRecentlyListenedRow(
                    tracks = recentTracks,
                    onTrackSelected = { track ->
                        playTrackFromList(
                            context = context,
                            track = track,
                            queueTracks = recentTracks
                        )
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(
                    14.dp
                )
            )
        }
    }
}

@Composable
private fun SearchTabContent(
    navController: NavHostController,
    selectedSource: LibrarySource,
    isLibraryLoading: Boolean,
    onSourceSelected: (LibrarySource) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchedTracks: List<MusicTrack>
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LibrarySourceSelector(
            selectedSource = selectedSource,
            isLoading = isLibraryLoading,
            onSourceSelected = onSourceSelected
        )

        LibrarySearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClose = {
                onSearchQueryChange(
                    ""
                )
            }
        )

        if (
            searchQuery.isNotBlank()
        ) {
            Text(
                text =
                if (
                    searchedTracks.size == 1
                ) {
                    "1 result"
                } else {
                    "${searchedTracks.size} results"
                },
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 4.dp
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    )
            ) {
                MusicLibraryScreen(
                    tracks = searchedTracks,
                    onOpenLyrics = { track ->
                        NowPlayingLyricsRequest.request(
                            track
                        )

                        navController.navigate(
                            Screen.NowPlaying.route
                        )
                    },
                    onOpenTrackInfo = { track ->
                        TrackInfoNavigationState.selectedTrack.value =
                            track

                        navController.navigate(
                            Screen.TrackInfo.route
                        )
                    }
                )
            }
        } else {
            Spacer(
                modifier = Modifier.weight(
                    1f
                )
            )
        }
    }
}

@Composable
private fun LibraryTabContent(
    context: Context,
    navController: NavHostController,
    selectedSource: LibrarySource,
    isLibraryLoading: Boolean,
    onSourceSelected: (LibrarySource) -> Unit,
    tracks: List<MusicTrack>,
    searchedTracks: List<MusicTrack>,
    libraryMode: LibraryMode,
    onLibraryModeChange: (LibraryMode) -> Unit,
    onOpenFilter: () -> Unit,
    selectedAlbum: String?,
    onSelectedAlbumChange: (String?) -> Unit,
    selectedArtist: String?,
    onSelectedArtistChange: (String?) -> Unit,
    selectedPlaylistId: String?,
    onSelectedPlaylistChange: (String?) -> Unit,
    librarySource: LibrarySource
) {
    val scope =
        rememberCoroutineScope()

    var artworkTracksToEdit by remember {
        mutableStateOf<List<MusicTrack>>(
            emptyList()
        )
    }

    var pendingArtworkUri by remember {
        mutableStateOf<Uri?>(
            null
        )
    }

    var pendingArtworkWritePermission by remember {
        mutableStateOf<PendingIntent?>(
            null
        )
    }

    var artworkEditInProgress by remember {
        mutableStateOf(
            false
        )
    }

    var artworkEditMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    suspend fun performArtworkWrite() {
        val imageUri =
            pendingArtworkUri
                ?: return

        val albumTracks =
            artworkTracksToEdit

        if (
            albumTracks.isEmpty()
        ) {
            return
        }

        artworkEditInProgress =
            true

        when (
            val result =
                AudioTagEditor.addAlbumArtwork(
                    context =
                    context.applicationContext,
                    tracks = albumTracks,
                    imageUri = imageUri
                )
        ) {
            is AudioTagEditResult.Success -> {
                applyUpdatedAudioTracks(
                    context =
                    context.applicationContext,
                    updatedTracks =
                    result.updatedTracks
                )

                artworkEditMessage =
                    result.message

                pendingArtworkUri =
                    null

                artworkTracksToEdit =
                    emptyList()
            }

            is AudioTagEditResult.PermissionRequired -> {
                pendingArtworkWritePermission =
                    result.pendingIntent
            }

            is AudioTagEditResult.Failure -> {
                artworkEditMessage =
                    result.message
            }
        }

        artworkEditInProgress =
            false
    }

    val artworkWritePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
            ActivityResultContracts
                .StartIntentSenderForResult()
        ) { activityResult ->
            if (
                activityResult.resultCode ==
                Activity.RESULT_OK
            ) {
                scope.launch {
                    performArtworkWrite()
                }
            } else {
                artworkEditMessage =
                    "Write permission was not granted."

                pendingArtworkUri =
                    null

                artworkTracksToEdit =
                    emptyList()
            }
        }

    LaunchedEffect(
        pendingArtworkWritePermission
    ) {
        val request =
            pendingArtworkWritePermission
                ?: return@LaunchedEffect

        pendingArtworkWritePermission =
            null

        artworkWritePermissionLauncher.launch(
            IntentSenderRequest
                .Builder(
                    request.intentSender
                )
                .build()
        )
    }

    val artworkPickerLauncher =
        rememberLauncherForActivityResult(
            contract =
            ActivityResultContracts
                .GetContent()
        ) { selectedImageUri ->
            if (
                selectedImageUri == null
            ) {
                return@rememberLauncherForActivityResult
            }

            pendingArtworkUri =
                selectedImageUri

            val permissionRequest =
                AudioTagEditor.createWriteRequest(
                    context = context,
                    tracks =
                    artworkTracksToEdit
                )

            if (
                permissionRequest != null
            ) {
                pendingArtworkWritePermission =
                    permissionRequest
            } else {
                scope.launch {
                    performArtworkWrite()
                }
            }
        }

    val categorySummary =
        remember(
            libraryMode,
            searchedTracks
        ) {
            when (
                libraryMode
            ) {
                LibraryMode.SONGS -> {
                    if (
                        searchedTracks.size == 1
                    ) {
                        "1 song"
                    } else {
                        "${searchedTracks.size} songs"
                    }
                }

                LibraryMode.ALBUMS -> {
                    val albumCount =
                        searchedTracks
                            .distinctBy { track ->
                                val albumArtist =
                                    track.albumArtist.ifBlank {
                                        track.artist
                                    }

                                track.album
                                    .trim()
                                    .lowercase() +
                                        "|" +
                                        albumArtist
                                            .trim()
                                            .lowercase()
                            }
                            .size

                    if (
                        albumCount == 1
                    ) {
                        "1 album"
                    } else {
                        "$albumCount albums"
                    }
                }

                LibraryMode.ARTISTS -> {
                    val artistCount =
                        searchedTracks
                            .map { track ->
                                track.albumArtist.ifBlank {
                                    track.artist
                                }
                            }
                            .map { artist ->
                                artist.trim()
                            }
                            .filter { artist ->
                                artist.isNotBlank()
                            }
                            .distinct()
                            .size

                    if (
                        artistCount == 1
                    ) {
                        "1 artist"
                    } else {
                        "$artistCount artists"
                    }
                }

                LibraryMode.PLAYLISTS -> {
                    "Browse playlists"
                }
            }
        }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LibrarySourceSelector(
            selectedSource = selectedSource,
            isLoading = isLibraryLoading,
            onSourceSelected = onSourceSelected
        )

        LibraryCategorySelector(
            libraryMode = libraryMode,
            onLibraryModeChange =
            onLibraryModeChange
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 10.dp,
                    top = 0.dp,
                    bottom = 6.dp
                ),
            verticalAlignment =
            Alignment.CenterVertically
        ) {
            Text(
                text = categorySummary,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.SemiBold,
                color = AuralArcStyle.TextSecondary
            )

            Spacer(
                modifier = Modifier.weight(
                    1f
                )
            )

            AuralArcIconButton(
                onClick = onOpenFilter,
                modifier = Modifier.size(
                    40.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Sort library",
                    tint = AuralArcStyle.TextPrimary,
                    modifier = Modifier.size(
                        22.dp
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(
                    1f
                )
        ) {
            when {
                selectedAlbum != null -> {
                    val albumTracks =
                        sortAlbumSongs(
                            searchedTracks.filter { track ->
                                track.album == selectedAlbum &&
                                        (
                                                selectedArtist == null ||
                                                        artistCategoryName(
                                                            track
                                                        ).equals(
                                                            selectedArtist,
                                                            ignoreCase = true
                                                        )
                                                )
                            }
                        )

                    val albumArtPath =
                        albumTracks
                            .firstOrNull {
                                !it.albumArtPath.isNullOrBlank()
                            }
                            ?.albumArtPath

                    Column {
                        AlbumDetailHeader(
                            albumName = selectedAlbum,
                            artistName = albumArtistText(
                                albumTracks
                            ),
                            songCount = albumTracks.size,
                            totalDuration = totalDurationText(
                                albumTracks
                            ),
                            albumArtPath = albumArtPath,
                            showAddArtwork =
                            librarySource ==
                                    LibrarySource.LOCAL &&
                                    albumArtPath.isNullOrBlank() &&
                                    albumTracks.isNotEmpty(),
                            onBack = {
                                onSelectedAlbumChange(
                                    null
                                )
                            },
                            onShufflePlay = {
                                shufflePlayTracks(
                                    context,
                                    albumTracks
                                )
                            },
                            onAddArtwork = {
                                artworkTracksToEdit =
                                    albumTracks

                                artworkPickerLauncher.launch(
                                    "image/*"
                                )
                            }
                        )

                        MusicLibraryScreen(
                            tracks = albumTracks,
                            onOpenLyrics = { track ->
                                NowPlayingLyricsRequest.request(
                                    track
                                )

                                navController.navigate(
                                    Screen.NowPlaying.route
                                )
                            },
                            onOpenTrackInfo = { track ->
                                TrackInfoNavigationState.selectedTrack.value =
                                    track

                                navController.navigate(
                                    Screen.TrackInfo.route
                                )
                            }
                        )
                    }
                }

                selectedArtist != null -> {
                    val artistTracks =
                        searchedTracks.filter { track ->
                            artistCategoryName(
                                track
                            ).equals(
                                selectedArtist,
                                ignoreCase = true
                            )
                        }

                    val artistTracksInOrder =
                        sortArtistSongsForPlayAll(
                            artistTracks
                        )

                    val artistAlbumCount =
                        artistTracks
                            .map {
                                it.album
                            }
                            .distinct()
                            .size

                    val artistArtPath =
                        artistTracks
                            .firstOrNull {
                                !it.albumArtPath.isNullOrBlank()
                            }
                            ?.albumArtPath

                    Column {
                        ArtistDetailHeader(
                            artistName = selectedArtist,
                            albumCount = artistAlbumCount,
                            songCount = artistTracks.size,
                            totalDuration = totalDurationText(
                                artistTracks
                            ),
                            artistArtPath = artistArtPath,
                            onBack = {
                                onSelectedArtistChange(
                                    null
                                )
                            },
                            onPlayAll = {
                                playTracks(
                                    context,
                                    artistTracks
                                )
                            },
                            onShufflePlay = {
                                shufflePlayTracks(
                                    context,
                                    artistTracks
                                )
                            }
                        )

                        ArtistAlbumsForArtistScreen(
                            artistTracks = artistTracks,
                            onAlbumSelected = { albumName ->
                                onSelectedAlbumChange(
                                    albumName
                                )
                            }
                        )
                    }
                }

                libraryMode == LibraryMode.SONGS -> {
                    MusicLibraryScreen(
                        tracks = searchedTracks,
                        onOpenLyrics = { track ->
                            NowPlayingLyricsRequest.request(
                                track
                            )

                            navController.navigate(
                                Screen.NowPlaying.route
                            )
                        },
                        onOpenTrackInfo = { track ->
                            TrackInfoNavigationState.selectedTrack.value =
                                track

                            navController.navigate(
                                Screen.TrackInfo.route
                            )
                        }
                    )
                }

                libraryMode == LibraryMode.ALBUMS -> {
                    AlbumBrowserScreen(
                        tracks = searchedTracks,
                        onAlbumSelected = { album ->
                            onSelectedAlbumChange(
                                album
                            )
                        }
                    )
                }

                libraryMode == LibraryMode.ARTISTS -> {
                    ArtistBrowserScreen(
                        tracks = searchedTracks,
                        onArtistSelected = { artist ->
                            onSelectedArtistChange(
                                artist
                            )
                        }
                    )
                }

                libraryMode == LibraryMode.PLAYLISTS -> {
                    if (
                        selectedPlaylistId == null
                    ) {
                        PlaylistBrowserScreen(
                            allTracks = tracks,
                            librarySource = librarySource,
                            onPlaylistSelected = { playlistId ->
                                onSelectedPlaylistChange(
                                    playlistId
                                )
                            }
                        )
                    } else {
                        PlaylistDetailScreen(
                            playlistId = selectedPlaylistId,
                            allTracks = tracks,
                            librarySource = librarySource,
                            onBack = {
                                onSelectedPlaylistChange(
                                    null
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (
        artworkEditInProgress
    ) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "Adding Album Artwork"
                )
            },
            text = {
                Row(
                    verticalAlignment =
                    Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier =
                        Modifier.size(
                            24.dp
                        ),
                        strokeWidth = 2.dp,
                        color =
                        AuralArcStyle.PurpleBright
                    )

                    Spacer(
                        modifier =
                        Modifier.width(
                            12.dp
                        )
                    )

                    Text(
                        text = "Writing the selected artwork into the album’s audio files…"
                    )
                }
            },
            confirmButton = {}
        )
    }

    artworkEditMessage?.let { message ->
        AlertDialog(
            onDismissRequest = {
                artworkEditMessage =
                    null
            },
            title = {
                Text(
                    text = "Album Artwork"
                )
            },
            text = {
                Text(
                    text = message
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        artworkEditMessage =
                            null
                    }
                ) {
                    Text(
                        text = "OK"
                    )
                }
            }
        )
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
        color = AuralArcStyle.TextPrimary,
        modifier = Modifier.padding(
            top = 8.dp,
            bottom = 2.dp
        )
    )

    Text(
        text = subtitle,
        style = MaterialTheme.typography.caption,
        color = AuralArcStyle.TextMuted,
        modifier = Modifier.padding(
            bottom = 8.dp
        )
    )
}

@Composable
private fun HorizontalRecentlyListenedRow(
    tracks: List<MusicTrack>,
    onTrackSelected: (MusicTrack) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 2.dp,
            end = 12.dp
        )
    ) {
        items(
            items = tracks,
            key = { track ->
                track.uri
            }
        ) { track ->
            HomeRecentlyListenedCard(
                track = track,
                onClick = {
                    onTrackSelected(
                        track
                    )
                }
            )
        }
    }
}

@Composable
private fun HorizontalTwoRowTrackGrid(
    tracks: List<MusicTrack>,
    onTrackSelected: (MusicTrack) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 2.dp,
            end = 12.dp
        )
    ) {
        items(
            items = tracks
                .take(
                    30
                )
                .chunked(
                    2
                ),
            key = { columnTracks ->
                columnTracks.joinToString(
                    "|"
                ) { track ->
                    track.uri
                }
            }
        ) { columnTracks ->
            Column(
                modifier = Modifier.width(
                    170.dp
                )
            ) {
                columnTracks.forEach { track ->
                    HomeTrackCard(
                        track = track,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                220.dp
                            ),
                        onClick = {
                            onTrackSelected(
                                track
                            )
                        }
                    )
                }

                if (
                    columnTracks.size == 1
                ) {
                    Spacer(
                        modifier = Modifier.height(
                            220.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTrackCard(
    track: MusicTrack,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(
                5.dp
            )
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AuralArcMotion.FAST,
                    easing = FastOutSlowInEasing
                )
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                10.dp
            )
        ) {
            TrackArtwork(
                albumArtPath = track.albumArtPath,
                size = 118.dp
            )

            Spacer(
                modifier = Modifier.height(
                    8.dp
                )
            )

            TrackTitleWithHdBadge(
                track = track,
                style = MaterialTheme.typography.body1,
                color = AuralArcStyle.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                fillTitleWeight = true
            )

            Text(
                text =
                trackArtistAlbumText(
                    track
                ),
                style = MaterialTheme.typography.caption,
                color = AuralArcStyle.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeRecentlyListenedCard(
    track: MusicTrack,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(
                170.dp
            )
            .padding(
                horizontal = 5.dp,
                vertical = 5.dp
            )
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = AuralArcMotion.FAST,
                    easing = FastOutSlowInEasing
                )
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                10.dp
            )
        ) {
            TrackArtwork(
                albumArtPath = track.albumArtPath,
                size = 118.dp
            )

            Spacer(
                modifier = Modifier.height(
                    8.dp
                )
            )

            TrackTitleWithHdBadge(
                track = track,
                style = MaterialTheme.typography.body1,
                color = AuralArcStyle.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                fillTitleWeight = true
            )

            Text(
                text =
                trackArtistAlbumText(
                    track
                ),
                style = MaterialTheme.typography.caption,
                color = AuralArcStyle.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeWideTrackCard(
    track: MusicTrack,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 5.dp
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(
                albumArtPath = track.albumArtPath,
                size = 54.dp
            )

            Spacer(
                modifier = Modifier.width(
                    12.dp
                )
            )

            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                TrackTitleWithHdBadge(
                    track = track,
                    style = MaterialTheme.typography.body1,
                    color = AuralArcStyle.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fillTitleWeight = true
                )

                Text(
                    text =
                    trackArtistAlbumText(
                        track
                    ),
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = AuralArcStyle.PurpleBright
            )
        }
    }
}

@Composable
private fun LibraryCategorySelector(
    libraryMode: LibraryMode,
    onLibraryModeChange: (LibraryMode) -> Unit
) {
    val modes =
        remember {
            listOf(
                LibraryMode.SONGS,
                LibraryMode.ALBUMS,
                LibraryMode.ARTISTS,
                LibraryMode.PLAYLISTS
            )
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(
            6.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val selected =
                libraryMode == mode

            Card(
                modifier = Modifier
                    .weight(
                        1f
                    )
                    .heightIn(
                        min = 42.dp
                    )
                    .auralArcClickable {
                        if (
                            !selected
                        ) {
                            onLibraryModeChange(
                                mode
                            )
                        }
                    },
                shape = AuralArcStyle.SmallShape,
                backgroundColor =
                if (
                    selected
                ) {
                    AuralArcStyle.PurpleDark
                } else {
                    AuralArcStyle.Surface
                },
                elevation =
                if (
                    selected
                ) {
                    6.dp
                } else {
                    0.dp
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 4.dp,
                            vertical = 11.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = libraryModeLabel(
                            mode
                        ),
                        style = MaterialTheme.typography.caption,
                        fontWeight =
                        if (
                            selected
                        ) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        },
                        color =
                        if (
                            selected
                        ) {
                            AuralArcStyle.TextPrimary
                        } else {
                            AuralArcStyle.TextMuted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterDialog(
    sortMode: TrackSortMode,
    onSortModeChange: (TrackSortMode) -> Unit,
    sortDescending: Boolean,
    onSortDescendingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sort Library"
            )
        },
        text = {
            Column {
                Text(
                    text = "Sort By",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(
                        6.dp
                    )
                )

                Row {
                    FilterChoiceButton(
                        text = "Title",
                        selected =
                        sortMode ==
                                TrackSortMode.TITLE,
                        onClick = {
                            onSortModeChange(
                                TrackSortMode.TITLE
                            )
                        },
                        modifier = Modifier.weight(
                            1f
                        )
                    )

                    Spacer(
                        modifier = Modifier.width(
                            6.dp
                        )
                    )

                    FilterChoiceButton(
                        text = "Artist",
                        selected =
                        sortMode ==
                                TrackSortMode.ARTIST,
                        onClick = {
                            onSortModeChange(
                                TrackSortMode.ARTIST
                            )
                        },
                        modifier = Modifier.weight(
                            1f
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(
                        6.dp
                    )
                )

                Row {
                    FilterChoiceButton(
                        text = "Album",
                        selected =
                        sortMode ==
                                TrackSortMode.ALBUM,
                        onClick = {
                            onSortModeChange(
                                TrackSortMode.ALBUM
                            )
                        },
                        modifier = Modifier.weight(
                            1f
                        )
                    )

                    Spacer(
                        modifier = Modifier.width(
                            6.dp
                        )
                    )

                    FilterChoiceButton(
                        text = "Release Date",
                        selected =
                        sortMode ==
                                TrackSortMode.RELEASE_DATE,
                        onClick = {
                            onSortModeChange(
                                TrackSortMode.RELEASE_DATE
                            )
                        },
                        modifier = Modifier.weight(
                            1f
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(
                        14.dp
                    )
                )

                Text(
                    text = "Order",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(
                        6.dp
                    )
                )

                Row {
                    FilterChoiceButton(
                        text = "Ascending",
                        selected =
                        !sortDescending,
                        onClick = {
                            onSortDescendingChange(
                                false
                            )
                        },
                        modifier = Modifier.weight(
                            1f
                        )
                    )

                    Spacer(
                        modifier = Modifier.width(
                            6.dp
                        )
                    )

                    FilterChoiceButton(
                        text = "Descending",
                        selected =
                        sortDescending,
                        onClick = {
                            onSortDescendingChange(
                                true
                            )
                        },
                        modifier = Modifier.weight(
                            1f
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Done"
                )
            }
        }
    )
}

@Composable
private fun FilterChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(
                min = 42.dp
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.SmallShape,
        backgroundColor =
        if (
            selected
        ) {
            AuralArcStyle.PurpleDark
        } else {
            AuralArcStyle.Surface
        },
        elevation =
        if (
            selected
        ) {
            6.dp
        } else {
            0.dp
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color =
                if (
                    selected
                ) {
                    AuralArcStyle.TextPrimary
                } else {
                    AuralArcStyle.TextMuted
                },
                style = MaterialTheme.typography.caption,
                fontWeight =
                if (
                    selected
                ) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
        }
    }
}

@Composable
private fun LibrarySourceSelector(
    selectedSource: LibrarySource,
    isLoading: Boolean,
    onSourceSelected: (LibrarySource) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            )
    ) {
        AuralArcButton(
            enabled = !isLoading,
            onClick = {
                onSourceSelected(
                    LibrarySource.LOCAL
                )
            },
            modifier = Modifier.weight(
                1f
            )
        ) {
            Text(
                text =
                if (
                    selectedSource == LibrarySource.LOCAL
                ) {
                    "Local On"
                } else {
                    "Local"
                }
            )
        }

        Spacer(
            modifier = Modifier.width(
                8.dp
            )
        )

        AuralArcButton(
            enabled = !isLoading,
            onClick = {
                onSourceSelected(
                    LibrarySource.NAVIDROME
                )
            },
            modifier = Modifier.weight(
                1f
            )
        ) {
            Text(
                text =
                if (
                    selectedSource == LibrarySource.NAVIDROME
                ) {
                    "Navidrome On"
                } else {
                    "Navidrome"
                }
            )
        }
    }
}

@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = 8.dp,
                bottom = 4.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = AuralArcStyle.PurpleBright
            )

            Spacer(
                modifier = Modifier.width(
                    8.dp
                )
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Songs, artists, or albums"
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(
                    1f
                )
            )

            if (
                query.isNotEmpty()
            ) {
                AuralArcIconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = AuralArcStyle.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(
    navController: NavHostController,
    selectedTab: LibraryRootTab,
    onTabSelected: (LibraryRootTab) -> Unit
) {
    Surface(
        elevation = 14.dp,
        color = AuralArcStyle.BackgroundBottom
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    bottom = 10.dp,
                    top = 6.dp
                )
        ) {
            if (
                PlayerManager.currentTitle.value.isNotEmpty()
            ) {
                MiniPlayerBar(
                    navController = navController
                )

                Divider(
                    color = AuralArcStyle.Divider,
                    modifier = Modifier.padding(
                        top = 8.dp
                    )
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp
                    ),
                shape = AuralArcStyle.CardShape,
                backgroundColor = AuralArcStyle.SurfaceBright,
                elevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RootTabButton(
                        tab = LibraryRootTab.HOME,
                        selected = selectedTab == LibraryRootTab.HOME,
                        icon = Icons.Default.Home,
                        onClick = {
                            onTabSelected(
                                LibraryRootTab.HOME
                            )
                        }
                    )

                    RootTabButton(
                        tab = LibraryRootTab.SEARCH,
                        selected = selectedTab == LibraryRootTab.SEARCH,
                        icon = Icons.Default.Search,
                        onClick = {
                            onTabSelected(
                                LibraryRootTab.SEARCH
                            )
                        }
                    )

                    RootTabButton(
                        tab = LibraryRootTab.LIBRARY,
                        selected = selectedTab == LibraryRootTab.LIBRARY,
                        icon = Icons.Default.LibraryMusic,
                        onClick = {
                            onTabSelected(
                                LibraryRootTab.LIBRARY
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.RootTabButton(
    tab: LibraryRootTab,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val alpha =
        animateFloatAsState(
            targetValue =
            if (
                selected
            ) {
                1f
            } else {
                0.62f
            },
            animationSpec = tween(
                durationMillis =
                AuralArcMotion.FAST
            )
        )

    Card(
        modifier = Modifier
            .weight(
                1f
            )
            .padding(
                horizontal = 4.dp
            )
            .alpha(
                alpha.value
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.SmallShape,
        backgroundColor =
        if (
            selected
        ) {
            AuralArcStyle.PurpleDark
        } else {
            AuralArcStyle.Surface
        },
        elevation =
        if (
            selected
        ) {
            8.dp
        } else {
            0.dp
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 8.dp,
                    horizontal = 4.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tab.label,
                tint =
                if (
                    selected
                ) {
                    AuralArcStyle.TextPrimary
                } else {
                    AuralArcStyle.TextMuted
                }
            )

            Text(
                text = tab.label,
                style = MaterialTheme.typography.caption,
                color =
                if (
                    selected
                ) {
                    AuralArcStyle.TextPrimary
                } else {
                    AuralArcStyle.TextMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun libraryModeLabel(
    mode: LibraryMode
): String {
    return when (
        mode
    ) {
        LibraryMode.SONGS ->
            "Songs"

        LibraryMode.ALBUMS ->
            "Albums"

        LibraryMode.ARTISTS ->
            "Artists"

        LibraryMode.PLAYLISTS ->
            "Playlists"
    }
}

@Composable
private fun AlbumDetailHeader(
    albumName: String,
    artistName: String,
    songCount: Int,
    totalDuration: String,
    albumArtPath: String?,
    showAddArtwork: Boolean,
    onBack: () -> Unit,
    onShufflePlay: () -> Unit,
    onAddArtwork: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                12.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuralArcIconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AuralArcStyle.TextPrimary
                    )
                }

                Text(
                    text = "Album",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    albumArtPath = albumArtPath,
                    size = 96.dp
                )

                Spacer(
                    modifier = Modifier.width(
                        14.dp
                    )
                )

                Column(
                    modifier = Modifier.weight(
                        1f
                    )
                ) {
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = AuralArcStyle.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "$songCount songs • $totalDuration",
                        style = MaterialTheme.typography.caption,
                        color = AuralArcStyle.TextMuted
                    )
                }

                Column(
                    horizontalAlignment =
                    Alignment.CenterHorizontally
                ) {
                    AuralArcIconButton(
                        onClick = onShufflePlay
                    ) {
                        Icon(
                            imageVector =
                            Icons.Default.Shuffle,
                            contentDescription =
                            "Shuffle play album",
                            tint =
                            AuralArcStyle.PurpleBright
                        )
                    }

                    if (
                        showAddArtwork
                    ) {
                        AlbumArtworkMoreOptionsButton(
                            onAddArtwork =
                            onAddArtwork
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArtworkMoreOptionsButton(
    onAddArtwork: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(
            false
        )
    }

    Box {
        AuralArcIconButton(
            onClick = {
                expanded =
                    true
            }
        ) {
            Icon(
                imageVector =
                Icons.Default.MoreVert,
                contentDescription =
                "Album options",
                tint =
                AuralArcStyle.TextPrimary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded =
                    false
            }
        ) {
            DropdownMenuItem(
                onClick = {
                    expanded =
                        false

                    onAddArtwork()
                }
            ) {
                Text(
                    text =
                    "Add Album Artwork"
                )
            }
        }
    }
}

@Composable
private fun ArtistDetailHeader(
    artistName: String,
    albumCount: Int,
    songCount: Int,
    totalDuration: String,
    artistArtPath: String?,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                12.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuralArcIconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AuralArcStyle.TextPrimary
                    )
                }

                Text(
                    text = "Artist",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    albumArtPath = artistArtPath,
                    size = 96.dp
                )

                Spacer(
                    modifier = Modifier.width(
                        14.dp
                    )
                )

                Column(
                    modifier = Modifier.weight(
                        1f
                    )
                ) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = AuralArcStyle.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "$albumCount albums • $songCount songs",
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextSecondary
                    )

                    Text(
                        text = totalDuration,
                        style = MaterialTheme.typography.caption,
                        color = AuralArcStyle.TextMuted
                    )
                }

                Row {
                    AuralArcIconButton(
                        onClick = onPlayAll
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play all artist songs in order",
                            tint = AuralArcStyle.PurpleBright
                        )
                    }

                    AuralArcIconButton(
                        onClick = onShufflePlay
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle play artist",
                            tint = AuralArcStyle.PurpleBright
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    navController: NavHostController
) {
    val duration =
        PlayerManager.duration.value

    val position =
        PlayerManager.currentPosition.value

    val progress =
        if (
            duration > 0L
        ) {
            (
                    position.toFloat() /
                            duration.toFloat()
                    ).coerceIn(
                    0f,
                    1f
                )
        } else {
            0f
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .auralArcClickable {
                navController.navigate(
                    Screen.NowPlaying.route
                )
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 12.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    albumArtPath = PlayerManager.currentAlbumArtPath.value,
                    size = 56.dp
                )

                Spacer(
                    modifier = Modifier.width(
                        12.dp
                    )
                )

                Column(
                    modifier = Modifier.weight(
                        1f
                    )
                ) {
                    Text(
                        text = PlayerManager.currentTitle.value,
                        fontWeight = FontWeight.Bold,
                        color = AuralArcStyle.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = PlayerManager.currentArtist.value,
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    modifier = Modifier.width(
                        8.dp
                    )
                )

                Card(
                    shape = AuralArcStyle.SmallShape,
                    backgroundColor =
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        AuralArcStyle.Purple
                    } else {
                        AuralArcStyle.SurfaceSoft
                    },
                    elevation = 6.dp
                ) {
                    IconButton(
                        onClick = {
                            if (
                                PlayerManager.isPlaying.value
                            ) {
                                PlayerManager.pause()
                            } else {
                                PlayerManager.resume()
                            }
                        }
                    ) {
                        Icon(
                            imageVector =
                            if (
                                PlayerManager.isPlaying.value
                            ) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription =
                            if (
                                PlayerManager.isPlaying.value
                            ) {
                                "Pause"
                            } else {
                                "Play"
                            },
                            tint = AuralArcStyle.TextPrimary
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        3.dp
                    ),
                color = AuralArcStyle.PurpleBright,
                backgroundColor = AuralArcStyle.Divider
            )
        }
    }
}