package com.keiranhaas.auralarc

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.keiranhaas.auralarc.navigation.AuralArcNavigation
import com.keiranhaas.auralarc.player.PlayerManager
import com.keiranhaas.auralarc.player.QueueManager
import com.keiranhaas.auralarc.storage.FolderManager
import com.keiranhaas.auralarc.storage.FolderPreferences
import com.keiranhaas.auralarc.ui.theme.AuralArcTheme
import com.keiranhaas.auralarc.utils.createExternalAudioTrack
import com.keiranhaas.auralarc.utils.getAudioUriFromIntent
import com.keiranhaas.auralarc.storage.LyricsPreferences
import com.keiranhaas.auralarc.ui.LibraryRuntimeState
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.keiranhaas.auralarc.storage.SetupPreferences
import com.keiranhaas.auralarc.storage.PowerSavingPreferences
import com.keiranhaas.auralarc.ui.setup.FirstRunSetupScreen
import com.keiranhaas.auralarc.utils.ArtworkBitmapLoader
import androidx.compose.runtime.mutableStateOf
import com.keiranhaas.auralarc.storage.ReleaseNotesPreferences
import com.keiranhaas.auralarc.ui.WhatsNewDialogHost
import com.keiranhaas.auralarc.ui.getAppVersionName

import com.keiranhaas.auralarc.widget.AuralArcWidgetUpdater

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_DRIVING_MODE =
            "com.keiranhaas.auralarc.OPEN_DRIVING_MODE"
    }

    private var appContentShown =
        false

    private val openDrivingModeRequest =
        mutableStateOf(false)

    private var pendingIntentToHandle: Intent? =
        null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        AuralArcWidgetUpdater.start(
            applicationContext
        )

        configureForegroundWindow()
        PowerSavingPreferences.initialize(this)

        preferHighRefreshRate()

        pendingIntentToHandle =
            intent

        openDrivingModeRequest.value =
            intent.getBooleanExtra(
                EXTRA_OPEN_DRIVING_MODE,
                false
            )

        if (openDrivingModeRequest.value) {
            (
                    getSystemService(
                        android.content.Context.NOTIFICATION_SERVICE
                    ) as android.app.NotificationManager
                    ).cancel(4102)
        }

        FolderManager.folders =
            FolderPreferences.loadFolders(
                this
            )

        if (
            SetupPreferences.shouldShowSetup(
                this
            )
        ) {
            showFirstRunSetup()
        } else {
            showApp()

            handleIncomingAudioIntent(
                pendingIntentToHandle
            )
        }
    }

    override fun onResume() {
        super.onResume()

        configureForegroundWindow()
    }

    override fun onStop() {
        /*
         * onStop is one of the last reliable Activity lifecycle
         * callbacks before Android may reclaim the UI process.
         *
         * Perform one synchronous position save here.
         */
        PlayerManager.saveCurrentSessionImmediately()

        super.onStop()
    }

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {
        super.onConfigurationChanged(
            newConfig
        )

        configureForegroundWindow()
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(
            intent
        )

        setIntent(
            intent
        )

        pendingIntentToHandle =
            intent

        openDrivingModeRequest.value =
            intent.getBooleanExtra(
                EXTRA_OPEN_DRIVING_MODE,
                false
            )

        if (openDrivingModeRequest.value) {
            (
                    getSystemService(
                        android.content.Context.NOTIFICATION_SERVICE
                    ) as android.app.NotificationManager
                    ).cancel(4102)
        }

        if (
            appContentShown ||
            !SetupPreferences.shouldShowSetup(
                this
            )
        ) {
            showApp()

            handleIncomingAudioIntent(
                intent
            )
        }
    }

    override fun onTrimMemory(
        level: Int
    ) {
        ArtworkBitmapLoader.trimMemory(
            level
        )

        super.onTrimMemory(
            level
        )
    }

    override fun onLowMemory() {
        ArtworkBitmapLoader.clearMemory()

        super.onLowMemory()
    }

    private fun configureForegroundWindow() {
        /*
         * Keep Compose content outside the system navigation-bar
         * inset while allowing the bar itself to be transparent.
         */
        WindowCompat.setDecorFitsSystemWindows(
            window,
            true
        )

        window.navigationBarColor =
            Color.TRANSPARENT

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O_MR1
        ) {
            window.navigationBarDividerColor =
                Color.TRANSPARENT
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            window.isNavigationBarContrastEnforced =
                false
        }

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).isAppearanceLightNavigationBars =
            false
    }

    private fun preferHighRefreshRate() {
        val layoutParams =
            window.attributes

        layoutParams.preferredRefreshRate =
            120f

        window.attributes =
            layoutParams
    }

    private fun showFirstRunSetup() {
        setContent {
            AuralArcTheme {
                FirstRunSetupScreen(
                    onFinished = {
                        SetupPreferences.markSetupComplete(
                            this@MainActivity
                        )

                        ReleaseNotesPreferences.markSeen(
                            this@MainActivity,
                            getAppVersionName(
                                this@MainActivity
                            )
                        )

                        showApp()

                        handleIncomingAudioIntent(
                            pendingIntentToHandle
                        )
                    }
                )
            }
        }
    }

    private fun showApp() {
        if (
            appContentShown
        ) {
            return
        }

        PlayerManager.restoreSavedDisplayState(
            this
        )

        LyricsPreferences
            .migrateLegacyLyricsFolderToPickedFolders(
                this
            )

        LibraryRuntimeState.restoreNavigationState(
            this
        )

        setContent {
            AuralArcTheme {
                AuralArcNavigation(
                    openDrivingModeRequest =
                        openDrivingModeRequest.value,
                    onDrivingModeRequestHandled = {
                        openDrivingModeRequest.value = false
                    }
                )
                WhatsNewDialogHost()
            }
        }

        PlayerManager.ensurePlaybackNotification(
            this
        )

        appContentShown =
            true
    }

    private fun handleIncomingAudioIntent(
        incomingIntent: Intent?
    ) {
        val audioUri =
            getAudioUriFromIntent(
                incomingIntent
            ) ?: return

        tryTakePersistableReadPermission(
            uri = audioUri,
            incomingIntent = incomingIntent
        )

        Log.d(
            "AuralArc",
            "Handling external audio intent: $audioUri"
        )

        val track =
            createExternalAudioTrack(
                context = this,
                uri = audioUri
            )

        val externalQueue =
            listOf(
                track
            )

        QueueManager.setQueue(
            externalQueue,
            track
        )

        PlayerManager.playTrack(
            context = this,
            track = track,
            queueTracks = externalQueue
        )
    }

    private fun tryTakePersistableReadPermission(
        uri: Uri,
        incomingIntent: Intent?
    ) {
        val flags =
            incomingIntent?.flags
                ?: return

        val readFlag =
            Intent.FLAG_GRANT_READ_URI_PERMISSION

        val persistableFlag =
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

        val hasReadPermission =
            flags and readFlag != 0

        val hasPersistablePermission =
            flags and persistableFlag != 0

        if (
            !hasReadPermission ||
            !hasPersistablePermission
        ) {
            return
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                readFlag
            )

            Log.d(
                "AuralArc",
                "Persistable URI permission saved for $uri"
            )
        } catch (e: Exception) {
            Log.d(
                "AuralArc",
                "Could not persist URI permission for $uri"
            )
        }
    }
}