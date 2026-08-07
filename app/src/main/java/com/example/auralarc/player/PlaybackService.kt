package com.example.auralarc.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.auralarc.MainActivity
import com.example.auralarc.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.net.URL

@UnstableApi
class PlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID =
            "auralarc_playback_channel"

        private const val CHANNEL_NAME =
            "AuralArc Playback"

        private const val NOTIFICATION_ID =
            1001

        private const val TAG =
            "AuralArcPlaybackService"

        private const val ACTION_SHUFFLE =
            "com.example.auralarc.notification.SHUFFLE"

        private const val ACTION_PREVIOUS =
            "com.example.auralarc.notification.PREVIOUS"

        private const val ACTION_PLAY_PAUSE =
            "com.example.auralarc.notification.PLAY_PAUSE"

        private const val ACTION_NEXT =
            "com.example.auralarc.notification.NEXT"

        private const val ACTION_REPEAT =
            "com.example.auralarc.notification.REPEAT"

        private const val ACTION_REFRESH_CONTROLS =
            "com.example.auralarc.notification.REFRESH_CONTROLS"

        private const val SESSION_ACTION_SHUFFLE =
            "com.example.auralarc.session.SHUFFLE"

        private const val SESSION_ACTION_PREVIOUS =
            "com.example.auralarc.session.PREVIOUS"

        private const val SESSION_ACTION_NEXT =
            "com.example.auralarc.session.NEXT"

        private const val SESSION_ACTION_REPEAT =
            "com.example.auralarc.session.REPEAT"

        fun requestControlRefresh(
            context: Context
        ) {
            val refreshIntent =
                Intent(
                    context.applicationContext,
                    PlaybackService::class.java
                ).apply {
                    action =
                        ACTION_REFRESH_CONTROLS
                }

            ContextCompat.startForegroundService(
                context.applicationContext,
                refreshIntent
            )
        }
    }

    private var mediaSession: MediaSession? =
        null

    private var mediaNotificationControllerFuture: ListenableFuture<MediaController>? =
        null

    private var attachedPlayer: Player? =
        null

    private var isForegroundService =
        false

    private var lastArtworkPath =
        ""

    private var lastArtworkBitmap: Bitmap? =
        null

    private val shuffleSessionCommand =
        SessionCommand(
            SESSION_ACTION_SHUFFLE,
            Bundle.EMPTY
        )

    private val previousSessionCommand =
        SessionCommand(
            SESSION_ACTION_PREVIOUS,
            Bundle.EMPTY
        )

    private val nextSessionCommand =
        SessionCommand(
            SESSION_ACTION_NEXT,
            Bundle.EMPTY
        )

    private val repeatSessionCommand =
        SessionCommand(
            SESSION_ACTION_REPEAT,
            Bundle.EMPTY
        )

    private val mediaSessionCallback =
        object : MediaSession.Callback {

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val defaultResult =
                    super.onConnect(
                        session,
                        controller
                    )

                if (
                    !defaultResult.isAccepted
                ) {
                    return defaultResult
                }

                val availableSessionCommands =
                    defaultResult
                        .availableSessionCommands
                        .buildUpon()
                        .add(
                            shuffleSessionCommand
                        )
                        .add(
                            previousSessionCommand
                        )
                        .add(
                            nextSessionCommand
                        )
                        .add(
                            repeatSessionCommand
                        )
                        .build()

                return MediaSession.ConnectionResult
                    .AcceptedResultBuilder(
                        session
                    )
                    .setAvailablePlayerCommands(
                        defaultResult.availablePlayerCommands
                    )
                    .setAvailableSessionCommands(
                        availableSessionCommands
                    )
                    .setMediaButtonPreferences(
                        createMediaButtonPreferences()
                    )
                    .build()
            }

            override fun onPostConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ) {
                super.onPostConnect(
                    session,
                    controller
                )

                try {
                    session.setMediaButtonPreferences(
                        controller,
                        createMediaButtonPreferences()
                    )
                } catch (
                    exception: Exception
                ) {
                    Log.d(
                        TAG,
                        "Could not apply media buttons to a connected controller.",
                        exception
                    )
                }
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                return when (
                    customCommand.customAction
                ) {
                    SESSION_ACTION_SHUFFLE -> {
                        PlaybackState.toggleShuffle()

                        PlayerManager.applyPlaybackModes()

                        refreshPlaybackNotificationControls()

                        Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS
                            )
                        )
                    }

                    SESSION_ACTION_PREVIOUS -> {
                        PlayerManager.previous(
                            this@PlaybackService
                        )

                        postPlaybackNotification()

                        Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS
                            )
                        )
                    }

                    SESSION_ACTION_NEXT -> {
                        PlayerManager.next(
                            this@PlaybackService
                        )

                        postPlaybackNotification()

                        Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS
                            )
                        )
                    }

                    SESSION_ACTION_REPEAT -> {
                        PlaybackState.cycleRepeatMode()

                        PlayerManager.applyRepeatOnly()

                        refreshPlaybackNotificationControls()

                        Futures.immediateFuture(
                            SessionResult(
                                SessionResult.RESULT_SUCCESS
                            )
                        )
                    }

                    else -> {
                        super.onCustomCommand(
                            session,
                            controller,
                            customCommand,
                            args
                        )
                    }
                }
            }
        }

    private val playerListener =
        object : Player.Listener {
            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                postPlaybackNotification()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                lastArtworkPath =
                    ""

                lastArtworkBitmap =
                    null

                postPlaybackNotification()
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                postPlaybackNotification()
            }
        }

    private fun connectMediaNotificationController() {
        val activeSession =
            mediaSession ?: return

        if (
            mediaNotificationControllerFuture != null
        ) {
            return
        }

        val connectionHints =
            Bundle().apply {
                putBoolean(
                    MediaController.KEY_MEDIA_NOTIFICATION_CONTROLLER_FLAG,
                    true
                )
            }

        mediaNotificationControllerFuture =
            MediaController.Builder(
                this,
                activeSession.token
            )
                .setConnectionHints(
                    connectionHints
                )
                .buildAsync()
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val player =
            PlayerManager.getPlayer(
                this
            )

        attachedPlayer =
            player

        mediaSession =
            MediaSession.Builder(
                this,
                player
            )
                .setCallback(
                    mediaSessionCallback
                )
                .setSessionActivity(
                    createContentIntent()
                )
                .setMediaButtonPreferences(
                    createMediaButtonPreferences()
                )
                .build()

        connectMediaNotificationController()

        player.addListener(
            playerListener
        )

        postPlaybackNotification()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        PlayerManager.getPlayer(
            this
        )

        when (
            intent?.action
        ) {
            ACTION_SHUFFLE -> {
                PlaybackState.toggleShuffle()

                PlayerManager.applyPlaybackModes()

                refreshPlaybackNotificationControls()
            }

            ACTION_PREVIOUS -> {
                PlayerManager.previous(
                    this
                )

                postPlaybackNotification()
            }

            ACTION_PLAY_PAUSE -> {
                if (
                    PlayerManager.isPlaying.value
                ) {
                    PlayerManager.pause()
                } else {
                    PlayerManager.resume()
                }

                postPlaybackNotification()
            }

            ACTION_NEXT -> {
                PlayerManager.next(
                    this
                )

                postPlaybackNotification()
            }

            ACTION_REPEAT -> {
                PlaybackState.cycleRepeatMode()

                PlayerManager.applyRepeatOnly()

                refreshPlaybackNotificationControls()
            }

            ACTION_REFRESH_CONTROLS -> {
                refreshPlaybackNotificationControls()
            }

            else -> {
                postPlaybackNotification()
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        PlayerManager.stopBecauseAppClosed(
            this
        )

        stopSelf()

        super.onTaskRemoved(
            rootIntent
        )
    }

    override fun onDestroy() {
        try {
            attachedPlayer?.removeListener(
                playerListener
            )
        } catch (_: Exception) {
        }

        attachedPlayer =
            null

        try {
            mediaNotificationControllerFuture?.let { controllerFuture ->
                MediaController.releaseFuture(
                    controllerFuture
                )
            }
        } catch (_: Exception) {
        }

        mediaNotificationControllerFuture =
            null

        try {
            mediaSession?.release()
        } catch (_: Exception) {
        }

        mediaSession =
            null

        if (
            isForegroundService
        ) {
            stopForeground(
                true
            )

            isForegroundService =
                false
        }

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    private fun createMediaButtonPreferences(): List<CommandButton> {
        val shuffleEnabled =
            PlaybackState.shuffleEnabled.value

        val repeatMode =
            PlaybackState.repeatMode.value

        val shuffleIconConstant =
            if (
                shuffleEnabled
            ) {
                CommandButton.ICON_SHUFFLE_ON
            } else {
                CommandButton.ICON_SHUFFLE_OFF
            }

        val shuffleIconResource =
            if (
                shuffleEnabled
            ) {
                R.drawable.ic_notification_shuffle
            } else {
                R.drawable.ic_notification_shuffle_off
            }

        val shuffleTitle =
            if (
                shuffleEnabled
            ) {
                "Shuffle On"
            } else {
                "Shuffle Off"
            }

        val repeatIconConstant =
            when (
                repeatMode
            ) {
                AuralArcRepeatMode.OFF ->
                    CommandButton.ICON_REPEAT_OFF

                AuralArcRepeatMode.ALL ->
                    CommandButton.ICON_REPEAT_ALL

                AuralArcRepeatMode.ONE ->
                    CommandButton.ICON_REPEAT_ONE
            }

        val repeatIconResource =
            when (
                repeatMode
            ) {
                AuralArcRepeatMode.OFF ->
                    R.drawable.ic_notification_repeat_off

                AuralArcRepeatMode.ALL ->
                    R.drawable.ic_notification_repeat

                AuralArcRepeatMode.ONE ->
                    R.drawable.ic_notification_repeat_one
            }

        return listOf(
            CommandButton.Builder(
                shuffleIconConstant
            )
                .setCustomIconResId(
                    shuffleIconResource
                )
                .setDisplayName(
                    shuffleTitle
                )
                .setSessionCommand(
                    shuffleSessionCommand
                )
                .setSlots(
                    CommandButton.SLOT_OVERFLOW
                )
                .build(),

            CommandButton.Builder(
                CommandButton.ICON_PREVIOUS
            )
                .setDisplayName(
                    "Previous"
                )
                .setSessionCommand(
                    previousSessionCommand
                )
                .setSlots(
                    CommandButton.SLOT_BACK
                )
                .build(),

            CommandButton.Builder(
                CommandButton.ICON_NEXT
            )
                .setDisplayName(
                    "Next"
                )
                .setSessionCommand(
                    nextSessionCommand
                )
                .setSlots(
                    CommandButton.SLOT_FORWARD
                )
                .build(),

            CommandButton.Builder(
                repeatIconConstant
            )
                .setCustomIconResId(
                    repeatIconResource
                )
                .setDisplayName(
                    PlaybackState.repeatText()
                )
                .setSessionCommand(
                    repeatSessionCommand
                )
                .setSlots(
                    CommandButton.SLOT_OVERFLOW
                )
                .build()
        )
    }

    private fun refreshPlaybackNotificationControls() {
        try {
            val activeSession =
                mediaSession

            val buttons =
                createMediaButtonPreferences()

            activeSession?.setMediaButtonPreferences(
                buttons
            )

            val notificationController =
                activeSession
                    ?.mediaNotificationControllerInfo

            if (
                activeSession != null &&
                notificationController != null
            ) {
                activeSession.setMediaButtonPreferences(
                    notificationController,
                    buttons
                )
            }
        } catch (
            exception: Exception
        ) {
            Log.e(
                TAG,
                "Could not refresh MediaSession buttons.",
                exception
            )
        }

        postPlaybackNotification()
    }

    private fun postPlaybackNotification() {
        val notification =
            buildPlaybackNotification()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        isForegroundService =
            true
    }

    private fun buildPlaybackNotification(): Notification {
        val title =
            PlayerManager.currentTitle.value.ifBlank {
                "AuralArc"
            }

        val artist =
            PlayerManager.currentArtist.value.ifBlank {
                "Unknown Artist"
            }

        val playPauseIcon =
            if (
                PlayerManager.isPlaying.value
            ) {
                android.R.drawable.ic_media_pause
            } else {
                android.R.drawable.ic_media_play
            }

        val playPauseTitle =
            if (
                PlayerManager.isPlaying.value
            ) {
                "Pause"
            } else {
                "Play"
            }

        val shuffleEnabled =
            PlaybackState.shuffleEnabled.value

        val repeatMode =
            PlaybackState.repeatMode.value

        val shuffleTitle =
            if (
                shuffleEnabled
            ) {
                "Shuffle On"
            } else {
                "Shuffle Off"
            }

        val shuffleIcon =
            if (
                shuffleEnabled
            ) {
                R.drawable.ic_notification_shuffle
            } else {
                R.drawable.ic_notification_shuffle_off
            }

        val repeatTitle =
            PlaybackState.repeatText()

        val repeatIcon =
            when (
                repeatMode
            ) {
                AuralArcRepeatMode.OFF ->
                    R.drawable.ic_notification_repeat_off

                AuralArcRepeatMode.ALL ->
                    R.drawable.ic_notification_repeat

                AuralArcRepeatMode.ONE ->
                    R.drawable.ic_notification_repeat_one
            }

        val notificationBuilder =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_media_play
                )
                .setContentTitle(
                    title
                )
                .setContentText(
                    artist
                )
                .setSubText(
                    "AuralArc"
                )
                .setContentIntent(
                    createContentIntent()
                )
                .setLargeIcon(
                    getCurrentArtwork()
                )
                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )
                .setPriority(
                    NotificationCompat.PRIORITY_LOW
                )
                .setCategory(
                    NotificationCompat.CATEGORY_TRANSPORT
                )
                .setOnlyAlertOnce(
                    true
                )
                .setOngoing(
                    PlayerManager.isPlaying.value
                )
                .addAction(
                    NotificationCompat.Action(
                        shuffleIcon,
                        shuffleTitle,
                        serviceActionPendingIntent(
                            ACTION_SHUFFLE,
                            2001
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_previous,
                        "Previous",
                        serviceActionPendingIntent(
                            ACTION_PREVIOUS,
                            2002
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        playPauseIcon,
                        playPauseTitle,
                        serviceActionPendingIntent(
                            ACTION_PLAY_PAUSE,
                            2003
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        android.R.drawable.ic_media_next,
                        "Next",
                        serviceActionPendingIntent(
                            ACTION_NEXT,
                            2004
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        repeatIcon,
                        repeatTitle,
                        serviceActionPendingIntent(
                            ACTION_REPEAT,
                            2005
                        )
                    )
                )

        val activeMediaSession =
            mediaSession

        if (
            activeMediaSession != null
        ) {
            notificationBuilder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(
                    activeMediaSession
                )
                    .setShowActionsInCompactView(
                        1,
                        2,
                        3
                    )
            )
        }

        return notificationBuilder.build()
    }

    private fun createContentIntent(): PendingIntent {
        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val flags =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            flags
        )
    }

    private fun serviceActionPendingIntent(
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent =
            Intent(
                this,
                PlaybackService::class.java
            ).apply {
                this.action =
                    action
            }

        val flags =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            PendingIntent.getForegroundService(
                this,
                requestCode,
                intent,
                flags
            )
        } else {
            PendingIntent.getService(
                this,
                requestCode,
                intent,
                flags
            )
        }
    }

    private fun createNotificationChannel() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        "AuralArc music playback controls"

                    setShowBadge(
                        false
                    )

                    lockscreenVisibility =
                        Notification.VISIBILITY_PUBLIC
                }

            val notificationManager =
                getSystemService(
                    NotificationManager::class.java
                )

            notificationManager.createNotificationChannel(
                channel
            )
        }
    }

    private fun getCurrentArtwork(): Bitmap? {
        val artworkPath =
            PlayerManager.currentAlbumArtPath.value

        if (
            artworkPath.isBlank()
        ) {
            return null
        }

        if (
            artworkPath == lastArtworkPath &&
            lastArtworkBitmap != null
        ) {
            return lastArtworkBitmap
        }

        lastArtworkPath =
            artworkPath

        lastArtworkBitmap =
            if (
                artworkPath.startsWith(
                    "http://"
                ) ||
                artworkPath.startsWith(
                    "https://"
                )
            ) {
                loadRemoteArtworkAsync(
                    artworkPath
                )

                null
            } else {
                loadLocalArtwork(
                    artworkPath
                )
            }

        return lastArtworkBitmap
    }

    private fun loadLocalArtwork(
        artworkPath: String
    ): Bitmap? {
        return try {
            val file =
                File(
                    artworkPath
                )

            if (
                !file.exists() ||
                file.length() <= 0L
            ) {
                return null
            }

            BitmapFactory.decodeFile(
                file.absolutePath
            )
        } catch (
            exception: Exception
        ) {
            Log.d(
                TAG,
                "Could not load local notification artwork.",
                exception
            )

            null
        }
    }

    private fun loadRemoteArtworkAsync(
        artworkUrl: String
    ) {
        Thread {
            try {
                val bitmap =
                    URL(
                        artworkUrl
                    ).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(
                            inputStream
                        )
                    }

                if (
                    bitmap != null &&
                    artworkUrl == lastArtworkPath
                ) {
                    lastArtworkBitmap =
                        bitmap

                    postPlaybackNotification()
                }
            } catch (
                exception: Exception
            ) {
                Log.d(
                    TAG,
                    "Could not load remote notification artwork.",
                    exception
                )
            }
        }.start()
    }
}