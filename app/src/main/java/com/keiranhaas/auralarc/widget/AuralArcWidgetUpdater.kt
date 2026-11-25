package com.keiranhaas.auralarc.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.glance.appwidget.updateAll
import com.keiranhaas.auralarc.player.AuralArcRepeatMode
import com.keiranhaas.auralarc.player.PlaybackState
import com.keiranhaas.auralarc.player.PlayerManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

object AuralArcWidgetUpdater {

    private data class PlaybackKey(
        val title: String,
        val artist: String,
        val artworkPath: String,
        val isPlaying: Boolean,
        val shuffleEnabled: Boolean,
        val repeatMode: AuralArcRepeatMode
    )

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main +
                    CoroutineExceptionHandler { _, error ->
                        Log.e(
                            "AuralArcWidget",
                            "Widget updater stopped",
                            error
                        )

                        watchJob =
                            null
                    }
        )

    private var watchJob: Job? =
        null

    @OptIn(FlowPreview::class)
    @Synchronized
    fun start(
        context: Context
    ) {
        if (
            watchJob?.isActive == true
        ) {
            return
        }

        val appContext =
            context.applicationContext

        @Suppress("UNUSED_VARIABLE")
        val warmUpPlayerState =
            PlayerManager.currentTitle.value

        @Suppress("UNUSED_VARIABLE")
        val warmUpPlaybackState =
            PlaybackState.shuffleEnabled.value

        Snapshot.notifyObjectsInitialized()

        watchJob =
            scope.launch {
                snapshotFlow {
                    PlaybackKey(
                        title = PlayerManager.currentTitle.value,
                        artist = PlayerManager.currentArtist.value,
                        artworkPath = PlayerManager.currentAlbumArtPath.value,
                        isPlaying = PlayerManager.isPlaying.value,
                        shuffleEnabled = PlaybackState.shuffleEnabled.value,
                        repeatMode = PlaybackState.repeatMode.value
                    )
                }
                    .distinctUntilChanged()
                    .debounce(
                        250.milliseconds
                    )
                    .collect {
                        updateAllWidgets(
                            appContext
                        )
                    }
            }
    }

    suspend fun updateAllWidgets(
        context: Context
    ) {
        AuralArcSmallWidget().updateAll(
            context
        )

        AuralArcMediumWidget().updateAll(
            context
        )

        AuralArcLargeWidget().updateAll(
            context
        )
    }
}