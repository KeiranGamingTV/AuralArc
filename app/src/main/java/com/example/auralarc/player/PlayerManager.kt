package com.example.auralarc.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.storage.AdvancedAudioPreferences
import com.example.auralarc.storage.AudioBehaviorPreferences
import org.json.JSONArray
import com.example.auralarc.data.TrackIdentity
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioMixerAttributes
import android.media.AudioAttributes as AndroidAudioAttributes
import android.os.SystemClock
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import com.example.auralarc.ui.LyricsState

@UnstableApi
object PlayerManager {

    private const val TAG =
        "AuralArc"

    private const val SESSION_PREFS_NAME =
        "auralarc_playback_session"

    private const val KEY_LAST_URI =
        "last_uri"

    private const val KEY_LAST_KEY =
        "last_key"

    private const val KEY_LAST_POSITION =
        "last_position"

    private const val KEY_LAST_TITLE =
        "last_title"

    private const val KEY_LAST_ARTIST =
        "last_artist"

    private const val KEY_LAST_ALBUM_ART =
        "last_album_art"

    private const val KEY_LAST_DURATION =
        "last_duration"

    private const val KEY_QUEUE_KEYS =
        "queue_keys"

    private const val KEY_SHUFFLE =
        "shuffle"

    private const val KEY_REPEAT =
        "repeat"

    private const val SEEK_SETTLE_TOLERANCE_MS =
        1_250L

    private const val SEEK_SETTLE_TIMEOUT_MS =
        5_000L

    private const val SESSION_POSITION_SAVE_INTERVAL_MS =
        2_000L

    private var player: ExoPlayer? =
        null

    private var appContext: Context? =
        null

    private var currentLyricsLoadJob: Job? =
        null

    private val playbackModeScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main.immediate
        )

    private var playbackModeJob: Job? =
        null

    private var audioManager: AudioManager? =
        null

    private var userVolume =
        1f

    private var lastAudioFocusPaused =
        false

    private var duckedForAudioFocus =
        false

    private val progressHandler =
        Handler(
            Looper.getMainLooper()
        )
    private var pendingSeekPosition: Long? =
        null

    private var pendingSeekStartedAtElapsedMs =
        0L

    private var lastSessionPositionSaveAtElapsedMs =
        0L

    private var isRestoringSession =
        false

    private var restoredSessionPosition =
        0L

    val currentTitle =
        mutableStateOf("")

    val currentArtist =
        mutableStateOf("")

    val currentAlbumArtPath =
        mutableStateOf("")

    val isPlaying =
        mutableStateOf(false)

    val currentPosition =
        mutableStateOf(0L)

    val duration =
        mutableStateOf(0L)

    private val audioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange: Int ->
            val context =
                appContext

            when (
                focusChange
            ) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    duckedForAudioFocus =
                        false

                    refreshEffectivePlayerVolume()

                    if (
                        context != null &&
                        lastAudioFocusPaused &&
                        AdvancedAudioPreferences.getResumeOnFocusGain(
                            context
                        )
                    ) {
                        lastAudioFocusPaused =
                            false

                        player?.play()

                        isPlaying.value =
                            player?.isPlaying ?: false

                        saveCurrentSessionIfNeeded(
                            force = true
                        )

                        ensurePlaybackNotification(
                            context
                        )
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (
                        context != null &&
                        AdvancedAudioPreferences.getDuckVolume(
                            context
                        )
                    ) {
                        duckedForAudioFocus =
                            true

                        refreshEffectivePlayerVolume()
                    } else {
                        if (
                            isPlaying.value
                        ) {
                            lastAudioFocusPaused =
                                true
                        }

                        pause()
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (
                        isPlaying.value
                    ) {
                        lastAudioFocusPaused =
                            true
                    }

                    pause()
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    duckedForAudioFocus =
                        false

                    refreshEffectivePlayerVolume()

                    lastAudioFocusPaused =
                        false

                    if (
                        context == null ||
                        AdvancedAudioPreferences.getPermanentAudioFocusChange(
                            context
                        )
                    ) {
                        pause()
                    }
                }
            }
        }

    private val progressRunnable =
        object : Runnable {
            override fun run() {
                val activePlayer =
                    player

                if (
                    activePlayer != null
                ) {
                    val actualPlayerPosition =
                        activePlayer.currentPosition.coerceAtLeast(
                            0L
                        )

                    val pendingPosition =
                        pendingSeekPosition

                    if (
                        pendingPosition != null
                    ) {
                        val currentElapsedTime =
                            SystemClock.elapsedRealtime()

                        val playerReachedRequestedPosition =
                            abs(
                                actualPlayerPosition -
                                        pendingPosition
                            ) <= SEEK_SETTLE_TOLERANCE_MS

                        val seekTimedOut =
                            currentElapsedTime -
                                    pendingSeekStartedAtElapsedMs >=
                                    SEEK_SETTLE_TIMEOUT_MS

                        if (
                            playerReachedRequestedPosition ||
                            seekTimedOut
                        ) {
                            pendingSeekPosition =
                                null

                            pendingSeekStartedAtElapsedMs =
                                0L

                            currentPosition.value =
                                actualPlayerPosition
                        } else {
                            /*
                             * Media3 may briefly continue reporting
                             * the old position while the seek is
                             * being processed or new data buffers.
                             *
                             * Keep the UI at the requested position
                             * until the player catches up.
                             */
                            currentPosition.value =
                                pendingPosition
                        }
                    } else {
                        currentPosition.value =
                            actualPlayerPosition
                    }

                    val playerDuration =
                        activePlayer.duration

                    if (
                        playerDuration > 0L &&
                        playerDuration != C.TIME_UNSET
                    ) {
                        duration.value =
                            playerDuration
                    }

                    isPlaying.value =
                        activePlayer.isPlaying

                    val currentElapsedTime =
                        SystemClock.elapsedRealtime()

                    if (
                        !isRestoringSession &&
                        currentElapsedTime -
                        lastSessionPositionSaveAtElapsedMs >=
                        SESSION_POSITION_SAVE_INTERVAL_MS
                    ) {
                        lastSessionPositionSaveAtElapsedMs =
                            currentElapsedTime

                        saveCurrentSessionIfNeeded(
                            force = false
                        )
                    }
                }

                progressHandler.postDelayed(
                    this,
                    100L
                )
            }
        }

    private fun buildCurrentQueueMediaItems(): List<MediaItem> {
        return QueueManager
            .getQueueEntries()
            .map { entry ->
                buildMediaItem(
                    entry
                )
            }
    }

    fun initialize(
        context: Context
    ) {
        appContext =
            context.applicationContext

        audioManager =
            context.applicationContext.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        if (
            player != null
        ) {
            applyAudioBehaviorPreferences(
                context.applicationContext
            )

            attachEqualizerToCurrentAudioSession(
                context.applicationContext
            )

            return
        }

        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000,
                    120_000,
                    250,
                    1_000
                )
                .setPrioritizeTimeOverSizeThresholds(
                    true
                )
                .build()

        player =
            ExoPlayer.Builder(
                context.applicationContext
            )
                .setLoadControl(
                    loadControl
                )
                .build()
                .also { exoPlayer ->
                    exoPlayer.addListener(
                        object : Player.Listener {
                            override fun onIsPlayingChanged(
                                isPlayingNow: Boolean
                            ) {
                                isPlaying.value =
                                    isPlayingNow

                                saveCurrentSessionIfNeeded(
                                    force = false
                                )
                            }

                            override fun onPositionDiscontinuity(
                                oldPosition: Player.PositionInfo,
                                newPosition: Player.PositionInfo,
                                reason: Int
                            ) {
                                if (
                                    reason == Player.DISCONTINUITY_REASON_SEEK
                                ) {
                                    val requestedPosition =
                                        pendingSeekPosition

                                    if (
                                        requestedPosition != null
                                    ) {
                                        /*
                                         * Do not clear pendingSeekPosition here.
                                         *
                                         * Media3's discontinuity callback can arrive before
                                         * currentPosition consistently reports the new
                                         * position, especially for network audio.
                                         */
                                        currentPosition.value =
                                            requestedPosition
                                    } else {
                                        currentPosition.value =
                                            newPosition.positionMs.coerceAtLeast(
                                                0L
                                            )
                                    }

                                    saveCurrentSessionIfNeeded(
                                        force = true
                                    )
                                }
                            }

                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int
                            ) {
                                pendingSeekPosition =
                                    null

                                pendingSeekStartedAtElapsedMs =
                                    0L

                                val mediaId =
                                    mediaItem?.mediaId

                                val queueIndex =
                                    QueueManager
                                        .getQueueEntries()
                                        .indexOfFirst {
                                            it.entryId ==
                                                    mediaId
                                        }

                                if (
                                    queueIndex >= 0
                                ) {
                                    QueueManager.setCurrentQueueIndex(
                                        queueIndex
                                    )
                                }

                                QueueManager.currentTrack()?.let { track ->
                                    updateCurrentTrackState(
                                        track = track,
                                        resetPosition = true
                                    )

                                    refreshAudioProcessingForCurrentTrack()
                                }

                                attachEqualizerToCurrentAudioSession(
                                    context.applicationContext
                                )

                                saveCurrentSessionIfNeeded(
                                    force = true
                                )
                            }

                            override fun onPlaybackStateChanged(
                                playbackState: Int
                            ) {
                                if (
                                    playbackState == Player.STATE_READY
                                ) {
                                    val playerDuration =
                                        exoPlayer.duration

                                    if (
                                        playerDuration > 0L &&
                                        playerDuration != C.TIME_UNSET
                                    ) {
                                        duration.value =
                                            playerDuration
                                    }

                                    if (
                                        isRestoringSession
                                    ) {
                                        val actualRestoredPosition =
                                            exoPlayer.currentPosition.coerceAtLeast(
                                                0L
                                            )

                                        currentPosition.value =
                                            if (
                                                actualRestoredPosition > 0L ||
                                                restoredSessionPosition == 0L
                                            ) {
                                                actualRestoredPosition
                                            } else {
                                                restoredSessionPosition
                                            }

                                        isRestoringSession =
                                            false

                                        lastSessionPositionSaveAtElapsedMs =
                                            SystemClock.elapsedRealtime()
                                    }

                                    attachEqualizerToCurrentAudioSession(
                                        context.applicationContext
                                    )
                                }

                                if (
                                    playbackState == Player.STATE_ENDED
                                ) {
                                    isPlaying.value =
                                        false

                                    saveCurrentSessionIfNeeded(
                                        force = true
                                    )
                                }
                            }

                            override fun onPlayerError(
                                error: PlaybackException
                            ) {
                                Log.e(
                                    TAG,
                                    "Player error",
                                    error
                                )
                            }
                        }
                    )
                }

        applyAudioBehaviorPreferences(
            context.applicationContext
        )

        applyRepeatModeToPlayer()

        attachEqualizerToCurrentAudioSession(
            context.applicationContext
        )

        startProgressUpdates()
    }

    fun getPlayer(
        context: Context
    ): ExoPlayer {
        initialize(
            context
        )

        return player ?: throw IllegalStateException(
            "Player was not initialized."
        )
    }

    fun restoreSavedDisplayState(
        context: Context
    ) {
        val prefs =
            context.applicationContext.getSharedPreferences(
                SESSION_PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedTitle =
            prefs.getString(
                KEY_LAST_TITLE,
                ""
            ).orEmpty()

        if (
            savedTitle.isBlank()
        ) {
            return
        }

        currentTitle.value =
            savedTitle

        currentArtist.value =
            prefs.getString(
                KEY_LAST_ARTIST,
                ""
            ).orEmpty()

        currentAlbumArtPath.value =
            prefs.getString(
                KEY_LAST_ALBUM_ART,
                ""
            ).orEmpty()

        currentPosition.value =
            prefs.getLong(
                KEY_LAST_POSITION,
                0L
            ).coerceAtLeast(
                0L
            )

        duration.value =
            prefs.getLong(
                KEY_LAST_DURATION,
                0L
            ).coerceAtLeast(
                0L
            )

        /*
         * The complete Media3 queue is restored after cached or
         * refreshed tracks become available.
         */
        isPlaying.value =
            false
    }

    fun canRestoreLastSession(): Boolean {
        val activePlayer =
            player

        /*
         * QueueManager and the displayed title may survive after
         * the activity/service is removed because the Android
         * process itself can remain alive.
         *
         * What matters is whether the current ExoPlayer actually
         * contains a playable Media3 playlist.
         */
        return activePlayer == null ||
                activePlayer.mediaItemCount == 0
    }

    fun playTrack(
        context: Context,
        track: MusicTrack,
        queueTracks: List<MusicTrack>
    ) {
        initialize(
            context
        )

        isRestoringSession =
            false

        restoredSessionPosition =
            0L

        pendingSeekPosition =
            null

        pendingSeekStartedAtElapsedMs =
            0L

        if (
            !requestAudioFocusIfNeeded(
                context
            )
        ) {
            return
        }

        SmartShuffleState.refresh(
            context.applicationContext
        )

        val cleanedQueue =
            queueTracks
                .ifEmpty {
                    listOf(
                        track
                    )
                }
                .distinctBy {
                    it.uri
                }

        val currentQueue =
            QueueManager.getQueue()

        val currentQueueKeys =
            currentQueue.map {
                stableTrackKey(
                    it
                )
            }

        val newQueueKeys =
            cleanedQueue.map {
                stableTrackKey(
                    it
                )
            }

        val currentQueueAlreadyMatches =
            currentQueueKeys == newQueueKeys

        if (
            currentQueueAlreadyMatches
        ) {
            QueueManager.setQueuePreservingOrder(
                cleanedQueue,
                track
            )
        } else {
            QueueManager.setQueue(
                cleanedQueue,
                track
            )
        }

        val actualQueue =
            QueueManager.getQueue()

        if (
            actualQueue.isEmpty()
        ) {
            return
        }

        val startIndex =
            QueueManager.currentIndex.coerceAtLeast(
                0
            )

        currentPosition.value =
            0L

        duration.value =
            track.duration.coerceAtLeast(
                0L
            )

        updateCurrentTrackState(
            track = QueueManager.currentTrack() ?: track,
            resetPosition = true
        )

        refreshAudioProcessingForCurrentTrack()

        player?.apply {
            setMediaItems(
                buildCurrentQueueMediaItems(),
                startIndex,
                0L
            )

            prepare()

            attachEqualizerToCurrentAudioSession(
                context.applicationContext
            )

            play()
        }

        currentPosition.value =
            player?.currentPosition?.coerceAtLeast(
                0L
            ) ?: 0L

        refreshAudioProcessingForCurrentTrack()

        startPlaybackService(
            context.applicationContext
        )

        saveCurrentSessionIfNeeded(
            force = true
        )
    }

    fun playSong(
        context: Context,
        track: MusicTrack
    ) {
        PlaybackState.shuffleEnabled.value =
            false

        QueueManager.setQueuePreservingOrder(
            listOf(
                track
            ),
            track
        )

        playTrack(
            context = context,
            track = track,
            queueTracks = listOf(
                track
            )
        )
    }

    fun next(
        context: Context
    ) {
        initialize(
            context
        )

        val nextTrack =
            QueueManager.nextTrack(
                fromAuto = false
            ) ?: return

        val nextIndex =
            QueueManager.currentIndex

        if (
            nextIndex >= 0 &&
            nextIndex < QueueManager.getQueue().size
        ) {
            player?.seekTo(
                nextIndex,
                0L
            )

            player?.play()
        } else {
            syncQueueAfterQueueChange(
                context = context,
                resetPosition = true
            )
        }

        updateCurrentTrackState(
            track = nextTrack,
            resetPosition = true
        )

        refreshAudioProcessingForCurrentTrack()

        attachEqualizerToCurrentAudioSession(
            context.applicationContext
        )

        saveCurrentSessionIfNeeded(
            force = true
        )

        ensurePlaybackNotification(
            context
        )
    }

    fun previous(
        context: Context
    ) {
        initialize(
            context
        )

        val activePlayer =
            player

        val currentPlayerPosition =
            activePlayer?.currentPosition ?: currentPosition.value

        if (
            currentPlayerPosition > 3_000L
        ) {
            seekTo(
                0L
            )

            ensurePlaybackNotification(
                context
            )

            return
        }

        val previousTrack =
            QueueManager.previousTrack() ?: run {
                seekTo(
                    0L
                )

                ensurePlaybackNotification(
                    context
                )

                return
            }

        val previousIndex =
            QueueManager.currentIndex

        if (
            previousIndex >= 0 &&
            previousIndex < QueueManager.getQueue().size
        ) {
            player?.seekTo(
                previousIndex,
                0L
            )

            player?.play()
        } else {
            syncQueueAfterQueueChange(
                context = context,
                resetPosition = true
            )
        }

        updateCurrentTrackState(
            track = previousTrack,
            resetPosition = true
        )

        refreshAudioProcessingForCurrentTrack()

        attachEqualizerToCurrentAudioSession(
            context.applicationContext
        )

        saveCurrentSessionIfNeeded(
            force = true
        )

        ensurePlaybackNotification(
            context
        )
    }

    fun pause() {
        player?.pause()

        isPlaying.value =
            false

        duckedForAudioFocus =
            false

        refreshEffectivePlayerVolume()

        saveCurrentSessionIfNeeded(
            force = true
        )

        appContext?.let { context ->
            ensurePlaybackNotification(
                context
            )
        }
    }

    fun resume() {
        val context =
            appContext

        if (
            context != null &&
            !requestAudioFocusIfNeeded(
                context
            )
        ) {
            return
        }

        refreshAudioProcessingForCurrentTrack()

        context?.let {
            attachEqualizerToCurrentAudioSession(
                it
            )
        }

        player?.play()

        isPlaying.value =
            player?.isPlaying ?: false

        saveCurrentSessionIfNeeded(
            force = true
        )

        context?.let {
            ensurePlaybackNotification(
                it
            )
        }
    }

    fun seekTo(
        positionMillis: Long
    ) {
        val activePlayer =
            player

        val knownDuration =
            duration.value.takeIf {
                it > 0L
            }

        val safePosition =
            if (
                knownDuration != null
            ) {
                positionMillis.coerceIn(
                    0L,
                    knownDuration
                )
            } else {
                positionMillis.coerceAtLeast(
                    0L
                )
            }

        currentPosition.value =
            safePosition

        if (
            activePlayer == null
        ) {
            pendingSeekPosition =
                null

            pendingSeekStartedAtElapsedMs =
                0L

            return
        }

        pendingSeekPosition =
            safePosition

        pendingSeekStartedAtElapsedMs =
            SystemClock.elapsedRealtime()

        activePlayer.seekTo(
            safePosition
        )

        saveCurrentSessionIfNeeded(
            force = true
        )
    }

    fun setUserVolume(
        volume: Float
    ) {
        userVolume =
            volume.coerceIn(
                0f,
                1f
            )

        refreshEffectivePlayerVolume()
    }

    fun applyRepeatOnly() {
        applyRepeatModeToPlayer()

        refreshAudioProcessingForCurrentTrack()

        saveCurrentSessionIfNeeded(
            force = true
        )

        appContext?.let { context ->
            ensurePlaybackNotification(
                context
            )
        }
    }

    fun applyPlaybackModes() {
        /*
         * A second Shuffle press cancels work for the previous
         * requested state before it can install an outdated order.
         */
        playbackModeJob?.cancel()

        playbackModeJob =
            playbackModeScope.launch {
                val context =
                    appContext

                val requestedShuffleState =
                    PlaybackState
                        .shuffleEnabled
                        .value

                player?.shuffleModeEnabled =
                    false

                applyRepeatModeToPlayer()

                if (
                    context == null
                ) {
                    return@launch
                }

                val currentTrack =
                    QueueManager.currentTrack()

                val shuffleSnapshot =
                    QueueManager.createShuffleSnapshot(
                        currentTrack
                    )

                if (
                    currentTrack == null ||
                    shuffleSnapshot == null
                ) {
                    refreshAudioProcessingForCurrentTrack()

                    saveCurrentSessionIfNeeded(
                        force = true
                    )

                    ensurePlaybackNotification(
                        context
                    )

                    return@launch
                }

                val queueRevisionAtStart =
                    QueueManager.revision.value

                /*
                 * SharedPreferences and listening-stat JSON parsing
                 * happen on Dispatchers.IO rather than the UI thread.
                 */
                SmartShuffleState.refreshOffMain(
                    context
                )

                /*
                 * Smart Shuffle scoring and sorting happen on a
                 * background CPU dispatcher.
                 */
                val preparedEntries =
                    withContext(
                        Dispatchers.Default
                    ) {
                        if (
                            requestedShuffleState
                        ) {
                            val remainingEntries =
                                shuffleSnapshot
                                    .sourceEntries
                                    .filterNot { entry ->
                                        entry.entryId ==
                                                shuffleSnapshot
                                                    .currentEntry
                                                    .entryId
                                    }

                            buildList(
                                shuffleSnapshot
                                    .sourceEntries
                                    .size
                            ) {
                                add(
                                    shuffleSnapshot.currentEntry
                                )

                                addAll(
                                    SmartShuffleState
                                        .smartShuffleEntries(
                                            remainingEntries
                                        )
                                )
                            }
                        } else {
                            shuffleSnapshot.sourceEntries
                        }
                    }

                if (
                    !isActive
                ) {
                    return@launch
                }

                /*
                 * Do not apply an obsolete result when the user has
                 * changed the queue while the background sort ran.
                 */
                if (
                    QueueManager.revision.value !=
                    queueRevisionAtStart
                ) {
                    return@launch
                }

                if (
                    PlaybackState.shuffleEnabled.value !=
                    requestedShuffleState
                ) {
                    return@launch
                }

                QueueManager.applyPreparedShuffleOrder(
                    preparedEntries =
                    preparedEntries,
                    currentEntryId =
                    shuffleSnapshot
                        .currentEntry
                        .entryId
                )

                val appliedQueueRevision =
                    QueueManager.revision.value

                val desiredEntries =
                    QueueManager.getQueueEntries()

                /*
                 * Building MediaMetadata and MediaItems for thousands
                 * of tracks is also moved away from the main thread.
                 */
                val preparedMediaItems =
                    withContext(
                        Dispatchers.Default
                    ) {
                        desiredEntries.map { entry ->
                            buildMediaItem(
                                entry
                            )
                        }
                    }

                if (
                    !isActive ||
                    QueueManager.revision.value !=
                    appliedQueueRevision
                ) {
                    return@launch
                }

                /*
                 * Give Compose an opportunity to draw the changed
                 * Shuffle button before Media3 applies its timeline.
                 */
                yield()

                val activePlayer =
                    player

                if (
                    activePlayer != null &&
                    desiredEntries.isNotEmpty()
                ) {
                    try {
                        syncPlayerPlaylistWithoutInterrupting(
                            activePlayer =
                            activePlayer,
                            actualQueue =
                            desiredEntries.map { entry ->
                                entry.track
                            },
                            currentTrack =
                            currentTrack,
                            preparedEntries =
                            desiredEntries,
                            preparedMediaItems =
                            preparedMediaItems
                        )
                    } catch (
                        exception: Exception
                    ) {
                        Log.e(
                            TAG,
                            "Shuffle playlist sync failed",
                            exception
                        )

                        replacePlayerPlaylistKeepingPosition(
                            activePlayer =
                            activePlayer,
                            actualQueue =
                            desiredEntries,
                            preparedMediaItems =
                            preparedMediaItems
                        )
                    }
                }

                updateCurrentTrackState(
                    track =
                    currentTrack,
                    resetPosition =
                    false
                )

                refreshAudioProcessingForCurrentTrack()

                saveCurrentSessionIfNeeded(
                    force = true
                )

                ensurePlaybackNotification(
                    context
                )
            }
    }

    fun addToQueue(
        context: Context,
        track: MusicTrack
    ) {
        initialize(
            context
        )

        QueueManager.addToQueue(
            track
        )

        syncQueueAfterQueueChange(
            context = context,
            resetPosition = false
        )
    }

    fun playNext(
        context: Context,
        track: MusicTrack
    ) {
        initialize(
            context
        )

        QueueManager.playNext(
            track
        )

        syncQueueAfterQueueChange(
            context = context,
            resetPosition = false
        )
    }

    fun syncQueueAfterQueueChange(
        context: Context,
        resetPosition: Boolean = false
    ) {
        initialize(
            context
        )

        val actualQueue =
            QueueManager.getQueue()

        if (
            actualQueue.isEmpty()
        ) {
            clearQueuePlayback(
                context
            )

            return
        }

        val currentQueueTrack =
            QueueManager.currentTrack()

        if (
            currentQueueTrack == null
        ) {
            clearQueuePlayback(
                context
            )

            return
        }

        val activeMediaId =
            player?.currentMediaItem?.mediaId

        val queueMediaId =
            QueueManager.getCurrentEntryId()

        val currentSongDidNotChange =
            activeMediaId != null &&
                    activeMediaId == queueMediaId

        if (
            currentSongDidNotChange &&
            !resetPosition
        ) {
            syncPlayerPlaylistWithoutInterrupting(
                activePlayer = player ?: return,
                actualQueue = actualQueue,
                currentTrack = currentQueueTrack
            )

            updateCurrentTrackState(
                track = currentQueueTrack,
                resetPosition = false
            )

            refreshAudioProcessingForCurrentTrack()

            saveCurrentSessionIfNeeded(
                force = true
            )

            ensurePlaybackNotification(
                context
            )

            return
        }

        val wasPlaying =
            player?.isPlaying ?: false

        val savedPosition =
            if (
                resetPosition
            ) {
                0L
            } else {
                player?.currentPosition ?: currentPosition.value
            }

        val startIndex =
            QueueManager.currentIndex.coerceAtLeast(
                0
            )

        player?.apply {
            setMediaItems(
                buildCurrentQueueMediaItems(),
                startIndex,
                savedPosition.coerceAtLeast(
                    0L
                )
            )

            prepare()

            attachEqualizerToCurrentAudioSession(
                context.applicationContext
            )

            if (
                wasPlaying
            ) {
                play()
            }
        }

        QueueManager.currentTrack()?.let { track ->
            updateCurrentTrackState(
                track = track,
                resetPosition = resetPosition
            )
        }

        refreshAudioProcessingForCurrentTrack()

        saveCurrentSessionIfNeeded(
            force = true
        )

        ensurePlaybackNotification(
            context
        )
    }

    fun syncPlayerPlaylistToQueueOrder(
        context: Context
    ) {
        initialize(
            context
        )

        val actualQueue =
            QueueManager.getQueueEntries()

        if (
            actualQueue.isEmpty()
        ) {
            return
        }

        val activePlayer =
            player ?: return

        val currentEntryId =
            activePlayer
                .currentMediaItem
                ?.mediaId
                ?: QueueManager.getCurrentEntryId()
                ?: return

        val currentIndex =
            actualQueue.indexOfFirst {
                it.entryId ==
                        currentEntryId
            }

        if (
            currentIndex < 0
        ) {
            syncQueueAfterQueueChange(
                context = context,
                resetPosition = false
            )

            return
        }

        QueueManager.setCurrentQueueIndex(
            currentIndex
        )

        val currentTrack =
            actualQueue
                .getOrNull(
                    currentIndex
                )
                ?.track
                ?: return

        syncPlayerPlaylistWithoutInterrupting(
            activePlayer = activePlayer,
            actualQueue = QueueManager.getQueue(),
            currentTrack = currentTrack
        )

        actualQueue
            .getOrNull(
                currentIndex
            )
            ?.track
            ?.let { track ->
                updateCurrentTrackState(
                    track = track,
                    resetPosition = false
                )
            }

        refreshAudioProcessingForCurrentTrack()

        attachEqualizerToCurrentAudioSession(
            context.applicationContext
        )

        saveCurrentSessionIfNeeded(
            force = true
        )

        ensurePlaybackNotification(
            context
        )
    }

    fun clearQueuePlayback(
        context: Context
    ) {
        initialize(
            context
        )

        try {
            player?.stop()
            player?.clearMediaItems()
        } catch (_: Exception) {
        }

        try {
            EqualizerManager.release()
        } catch (_: Exception) {
        }

        QueueManager.clearQueue()

        currentTitle.value =
            ""

        currentArtist.value =
            ""

        currentAlbumArtPath.value =
            ""

        currentPosition.value =
            0L

        duration.value =
            0L

        isPlaying.value =
            false

        clearSavedSession(
            context
        )
    }

    fun restoreLastSession(
        context: Context,
        allTracks: List<MusicTrack>
    ) {
        initialize(
            context
        )

        if (
            !canRestoreLastSession()
        ) {
            return
        }

        if (
            allTracks.isEmpty()
        ) {
            return
        }

        val prefs =
            context.applicationContext.getSharedPreferences(
                SESSION_PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val lastKey =
            prefs.getString(
                KEY_LAST_KEY,
                null
            )

        val lastUri =
            prefs.getString(
                KEY_LAST_URI,
                null
            )

        if (
            lastKey.isNullOrBlank() &&
            lastUri.isNullOrBlank()
        ) {
            return
        }

        val trackByKey =
            allTracks.associateBy {
                TrackIdentity.getStableId(
                    it
                )
            }

        val trackByUri =
            allTracks.associateBy {
                it.uri
            }

        val startTrack =
            lastKey
                ?.let {
                    trackByKey[it]
                }
                ?: lastUri?.let {
                    trackByUri[it]
                }
                ?: return

        val queueKeys =
            readStringArray(
                prefs.getString(
                    KEY_QUEUE_KEYS,
                    "[]"
                ) ?: "[]"
            )

        val restoredQueue =
            queueKeys.mapNotNull { key ->
                trackByKey[key]
            }.ifEmpty {
                listOf(
                    startTrack
                )
            }

        val position =
            prefs.getLong(
                KEY_LAST_POSITION,
                0L
            ).coerceAtLeast(
                0L
            )

        isRestoringSession =
            true

        restoredSessionPosition =
            position

        pendingSeekPosition =
            null

        pendingSeekStartedAtElapsedMs =
            0L

        val repeatRaw =
            prefs.getString(
                KEY_REPEAT,
                AuralArcRepeatMode.OFF.name
            ) ?: AuralArcRepeatMode.OFF.name

        PlaybackState.shuffleEnabled.value =
            prefs.getBoolean(
                KEY_SHUFFLE,
                false
            )

        PlaybackState.repeatMode.value =
            try {
                AuralArcRepeatMode.valueOf(
                    repeatRaw
                )
            } catch (_: Exception) {
                AuralArcRepeatMode.OFF
            }

        QueueManager.setQueuePreservingOrder(
            restoredQueue,
            startTrack
        )

        val startIndex =
            QueueManager.currentIndex.coerceAtLeast(
                0
            )

        player?.apply {
            setMediaItems(
                buildCurrentQueueMediaItems(),
                startIndex,
                position.coerceAtLeast(
                    0L
                )
            )

            prepare()

            attachEqualizerToCurrentAudioSession(
                context.applicationContext
            )

            pause()
        }

        updateCurrentTrackState(
            track = startTrack,
            resetPosition = false
        )

        currentPosition.value =
            position

        isPlaying.value =
            false

        applyRepeatModeToPlayer()

        refreshAudioProcessingForCurrentTrack()

        startPlaybackService(
            context.applicationContext
        )
    }

    fun applyAudioBehaviorPreferences(
        context: Context
    ) {
        val activePlayer =
            player ?: return

        audioManager =
            context.applicationContext.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        activePlayer.setHandleAudioBecomingNoisy(
            AudioBehaviorPreferences.getPauseOnHeadphoneDisconnect(
                context
            )
        )

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_MUSIC
                )
                .build()

        activePlayer.setAudioAttributes(
            audioAttributes,
            false
        )
    }

    fun ensurePlaybackNotification(
        context: Context
    ) {
        initialize(
            context
        )

        val activePlayer =
            player
                ?: return

        if (
            activePlayer.mediaItemCount == 0
        ) {
            /*
             * Do not start PlaybackService using stale title and
             * QueueManager state before restoreLastSession() has
             * rebuilt the Media3 playlist.
             */
            return
        }

        if (
            currentTitle.value.isBlank() &&
            QueueManager.currentTrack() == null
        ) {
            return
        }

        startPlaybackService(
            context.applicationContext
        )
    }

    fun stopBecauseAppClosed(
        context: Context
    ) {
        if (
            !AudioBehaviorPreferences.getStopWhenAppClosed(
                context
            )
        ) {
            return
        }

        saveCurrentSessionIfNeeded(
            force = true
        )

        playbackModeJob?.cancel()

        playbackModeJob =
            null

        try {
            player?.pause()
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }

        try {
            EqualizerManager.release()
        } catch (_: Exception) {
        }

        player =
            null

        isPlaying.value =
            false

        progressHandler.removeCallbacks(
            progressRunnable
        )

        try {
            context.stopService(
                Intent(
                    context,
                    PlaybackService::class.java
                )
            )
        } catch (_: Exception) {
        }
    }

    fun saveCurrentSessionIfNeeded(
        force: Boolean = false
    ) {
        val context =
            appContext ?: return

        if (
            isRestoringSession
        ) {
            return
        }

        val currentTrack =
            QueueManager.currentTrack() ?: return

        val queue =
            QueueManager.getQueue()

        if (
            queue.isEmpty() &&
            !force
        ) {
            return
        }

        val queueKeysArray =
            JSONArray()

        queue.forEach { track ->
            queueKeysArray.put(
                stableTrackKey(
                    track
                )
            )
        }

        val positionToSave =
            (
                    pendingSeekPosition
                        ?: player?.currentPosition
                        ?: currentPosition.value
                    ).coerceAtLeast(
                    0L
                )

        val durationToSave =
            player
                ?.duration
                ?.takeIf { playerDuration ->
                    playerDuration > 0L &&
                            playerDuration != C.TIME_UNSET
                }
                ?: duration.value.takeIf {
                    it > 0L
                }
                ?: currentTrack.duration.coerceAtLeast(
                    0L
                )

        context.getSharedPreferences(
            SESSION_PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_LAST_URI,
                currentTrack.uri
            )
            .putString(
                KEY_LAST_KEY,
                stableTrackKey(
                    currentTrack
                )
            )
            .putLong(
                KEY_LAST_POSITION,
                positionToSave
            )
            .putString(
                KEY_LAST_TITLE,
                currentTrack.title
            )
            .putString(
                KEY_LAST_ARTIST,
                currentTrack.artist
            )
            .putString(
                KEY_LAST_ALBUM_ART,
                currentTrack.albumArtPath
                    ?: currentAlbumArtPath.value
            )
            .putLong(
                KEY_LAST_DURATION,
                durationToSave
            )
            .putString(
                KEY_QUEUE_KEYS,
                queueKeysArray.toString()
            )
            .putBoolean(
                KEY_SHUFFLE,
                PlaybackState.shuffleEnabled.value
            )
            .putString(
                KEY_REPEAT,
                PlaybackState.repeatMode.value.name
            )
            .apply()
    }

    private fun requestAudioFocusIfNeeded(
        context: Context
    ): Boolean {
        if (
            !AudioBehaviorPreferences.getUseAudioFocus(
                context
            )
        ) {
            return true
        }

        val result =
            audioManager?.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ||
                result == null
    }

    private fun refreshAudioProcessingForCurrentTrack() {
        val context =
            appContext
                ?: return

        AudioProcessingState.refresh(
            context = context,
            track = QueueManager.currentTrack()
        )

        refreshEffectivePlayerVolume()
    }

    private fun refreshEffectivePlayerVolume() {
        val context =
            appContext

        if (
            context != null
        ) {
            player?.volume =
                1f

            return
        }

        val replayGainMultiplier =
            AudioProcessingState.replayGainMultiplier.value

        val duckMultiplier =
            if (
                duckedForAudioFocus
            ) {
                0.25f
            } else {
                1f
            }

        val finalVolume =
            (
                    userVolume *
                            replayGainMultiplier *
                            duckMultiplier
                    ).coerceIn(
                    0f,
                    1f
                )

        player?.volume =
            finalVolume
    }

    private fun attachEqualizerToCurrentAudioSession(
        context: Context
    ) {

        val sessionId =
            try {
                player?.audioSessionId ?: 0
            } catch (_: Exception) {
                0
            }

        if (
            sessionId == 0
        ) {
            return
        }

        EqualizerManager.attachToAudioSession(
            context = context.applicationContext,
            audioSessionId = sessionId
        )
    }

    private fun buildMediaItem(
        entry: QueueEntry
    ): MediaItem {
        val track =
            entry.track

        val metadataBuilder =
            MediaMetadata.Builder()
                .setTitle(
                    track.title
                )
                .setArtist(
                    track.artist
                )
                .setAlbumTitle(
                    track.album
                )

        val artworkPath =
            track.albumArtPath

        if (
            !artworkPath.isNullOrBlank() &&
            (
                    artworkPath.startsWith(
                        "http://"
                    ) ||
                            artworkPath.startsWith(
                                "https://"
                            )
                    )
        ) {
            metadataBuilder.setArtworkUri(
                Uri.parse(
                    artworkPath
                )
            )
        }

        return MediaItem.Builder()
            .setUri(
                track.uri
            )
            .setMediaId(
                entry.entryId
            )
            .setMediaMetadata(
                metadataBuilder.build()
            )
            .setTag(
                TrackIdentity.getStableId(
                    track
                )
            )
            .build()
    }

    private fun syncPlayerPlaylistWithoutInterrupting(
        activePlayer: ExoPlayer,
        actualQueue: List<MusicTrack>,
        currentTrack: MusicTrack,
        preparedEntries: List<QueueEntry>? = null,
        preparedMediaItems: List<MediaItem>? = null
    ) {
        val desiredEntries =
            preparedEntries
                ?: QueueManager.getQueueEntries()

        if (
            actualQueue.isEmpty() ||
            desiredEntries.isEmpty()
        ) {
            return
        }

        val currentPlayerEntryId =
            activePlayer.currentMediaItem?.mediaId

        val currentPlayerIndex =
            activePlayer.currentMediaItemIndex

        val desiredCurrentIndex =
            desiredEntries.indexOfFirst { entry ->
                entry.entryId ==
                        currentPlayerEntryId
            }

        /*
         * The normal queue-sync path requires the current Media3
         * item to match an existing QueueEntry.
         *
         * If it does not, use the full playlist replacement as a
         * recovery path.
         */
        if (
            currentPlayerEntryId.isNullOrBlank() ||
            currentPlayerIndex < 0 ||
            desiredCurrentIndex < 0
        ) {
            replacePlayerPlaylistKeepingPosition(
                activePlayer =
                activePlayer,
                actualQueue =
                desiredEntries,
                preparedMediaItems =
                preparedMediaItems
            )

            return
        }

        val desiredMediaItems =
            preparedMediaItems
                ?: desiredEntries.map { entry ->
                    buildMediaItem(
                        entry
                    )
                }

        /*
         * Replace everything before the current item.
         *
         * The current item itself is not replaced. If the number
         * of songs before it changes, Media3 automatically shifts
         * the current item's index.
         */
        val desiredPrefix =
            desiredMediaItems.subList(
                0,
                desiredCurrentIndex
            )

        if (
            currentPlayerIndex > 0 ||
            desiredPrefix.isNotEmpty()
        ) {
            activePlayer.replaceMediaItems(
                0,
                currentPlayerIndex,
                desiredPrefix
            )
        }

        /*
         * After replacing the prefix, the current item is now at
         * desiredCurrentIndex.
         */
        val suffixStart =
            desiredCurrentIndex + 1

        val desiredSuffix =
            desiredMediaItems.subList(
                suffixStart,
                desiredMediaItems.size
            )

        if (
            suffixStart < activePlayer.mediaItemCount ||
            desiredSuffix.isNotEmpty()
        ) {
            activePlayer.replaceMediaItems(
                suffixStart,
                activePlayer.mediaItemCount,
                desiredSuffix
            )
        }

        QueueManager.setCurrentQueueIndex(
            desiredCurrentIndex
        )

        updateCurrentTrackState(
            track = currentTrack,
            resetPosition = false
        )

        refreshAudioProcessingForCurrentTrack()

        saveCurrentSessionIfNeeded(
            force = true
        )
    }

    private fun replacePlayerPlaylistKeepingPosition(
        activePlayer: ExoPlayer,
        actualQueue: List<QueueEntry>,
        preparedMediaItems: List<MediaItem>? = null
    ) {
        if (
            actualQueue.isEmpty()
        ) {
            return
        }

        val shouldResumePlayback =
            activePlayer.playWhenReady

        val savedPosition =
            activePlayer.currentPosition.coerceAtLeast(
                0L
            )

        val currentEntryId =
            QueueManager.getCurrentEntryId()

        val matchingIndex =
            actualQueue.indexOfFirst { entry ->
                entry.entryId ==
                        currentEntryId
            }

        val startIndex =
            if (
                matchingIndex >= 0
            ) {
                matchingIndex
            } else {
                QueueManager.currentIndex.coerceIn(
                    0,
                    actualQueue.lastIndex
                )
            }

        val mediaItems =
            preparedMediaItems
                ?: actualQueue.map { entry ->
                    buildMediaItem(
                        entry
                    )
                }

        activePlayer.setMediaItems(
            mediaItems,
            startIndex,
            savedPosition
        )

        activePlayer.prepare()

        activePlayer.playWhenReady =
            shouldResumePlayback
    }

    fun applyUpdatedTrackMetadata(
        updatedTracks: List<MusicTrack>
    ) {
        if (
            updatedTracks.isEmpty()
        ) {
            return
        }

        QueueManager.replaceTracks(
            updatedTracks
        )

        val currentTrack =
            QueueManager.currentTrack()
                ?: return

        updateCurrentTrackState(
            track = currentTrack,
            resetPosition = false
        )
    }

    private fun updateCurrentTrackState(
        track: MusicTrack,
        resetPosition: Boolean = false
    ) {
        currentTitle.value =
            track.title

        currentArtist.value =
            track.artist

        currentAlbumArtPath.value =
            track.albumArtPath ?: ""

        duration.value =
            track.duration.coerceAtLeast(
                0L
            )

        if (
            resetPosition
        ) {
            currentPosition.value =
                0L
        }

        currentLyricsLoadJob?.cancel()

        val lyricsContext =
            appContext

        if (
            lyricsContext != null
        ) {
            currentLyricsLoadJob =
                playbackModeScope.launch {
                    LyricsState.preloadLyrics(
                        context =
                            lyricsContext.applicationContext,
                        track =
                            track
                    )
                }
        }
    }

    private fun applyRepeatModeToPlayer() {
        player?.repeatMode =
            when (
                PlaybackState.repeatMode.value
            ) {
                AuralArcRepeatMode.OFF ->
                    Player.REPEAT_MODE_OFF

                AuralArcRepeatMode.ONE ->
                    Player.REPEAT_MODE_ONE

                AuralArcRepeatMode.ALL ->
                    Player.REPEAT_MODE_ALL
            }

        player?.shuffleModeEnabled =
            false
    }

    private fun startPlaybackService(
        context: Context
    ) {
        try {
            val intent =
                Intent(
                    context,
                    PlaybackService::class.java
                )

            Log.d(
                TAG,
                "Starting playback service"
            )

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ) {
                ContextCompat.startForegroundService(
                    context,
                    intent
                )
            } else {
                context.startService(
                    intent
                )
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to start playback service",
                e
            )
        }
    }

    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(
            progressRunnable
        )

        progressHandler.post(
            progressRunnable
        )
    }

    private fun clearSavedSession(
        context: Context
    ) {
        context.applicationContext.getSharedPreferences(
            SESSION_PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
    }

    private fun stableTrackKey(
        track: MusicTrack
    ): String {
        return TrackIdentity.getStableId(
            track
        )
    }

    private fun extractNavidromeSongId(
        trackUri: String
    ): String? {
        return try {
            val uri =
                Uri.parse(
                    trackUri
                )

            val path =
                uri.path ?: ""

            val looksLikeNavidrome =
                path.contains(
                    "stream.view"
                ) ||
                        path.contains(
                            "download.view"
                        )

            if (
                !looksLikeNavidrome
            ) {
                return null
            }

            uri.getQueryParameter(
                "id"
            )?.takeIf {
                it.isNotBlank()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readStringArray(
        rawJson: String
    ): List<String> {
        return try {
            val array =
                JSONArray(
                    rawJson
                )

            val values =
                mutableListOf<String>()

            for (
            index in 0 until array.length()
            ) {
                val value =
                    array.optString(
                        index,
                        ""
                    )

                if (
                    value.isNotBlank()
                ) {
                    values.add(
                        value
                    )
                }
            }

            values
        } catch (_: Exception) {
            emptyList()
        }
    }
}