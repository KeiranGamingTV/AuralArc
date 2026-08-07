package com.example.auralarc.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.auralarc.ui.*
import androidx.compose.runtime.LaunchedEffect
import com.example.auralarc.ui.TrackInfoNavigationState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.example.auralarc.ui.theme.AuralArcMotion

@Composable
fun AuralArcNavigation() {
    val navController =
        rememberNavController()

    ListeningStatsTracker()

    LyricsPreloadTracker()

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        enterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis =
                    AuralArcMotion.NORMAL
                )
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(
                    durationMillis =
                    AuralArcMotion.FAST
                )
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(
                    durationMillis =
                    AuralArcMotion.NORMAL
                )
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(
                    durationMillis =
                    AuralArcMotion.FAST
                )
            )
        }
    ) {
        composable(
            Screen.Library.route
        ) {
            MusicLibraryView(
                navController = navController
            )
        }

        composable(
            Screen.NowPlaying.route
        ) {
            NowPlayingScreen(
                navController = navController
            )
        }

        composable(
            Screen.Queue.route
        ) {
            QueueScreen(
                navController = navController
            )
        }

        composable(
            Screen.Settings.route
        ) {
            SettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.NavidromeSettings.route
        ) {
            NavidromeSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.LibraryFolderSettings.route
        ) {
            LibraryFolderSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.ListeningStats.route
        ) {
            ListeningStatsScreen(
                navController = navController
            )
        }

        composable(
            Screen.AppearanceSettings.route
        ) {
            AppearanceSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.AudioBehaviorSettings.route
        ) {
            AudioBehaviorSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.AboutSettings.route
        ) {
            AboutSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.TrackInfo.route
        ) {
            val selectedTrack = TrackInfoNavigationState.selectedTrack.value

            if (
                selectedTrack != null
            ) {
                TrackInfoScreen(
                    track = selectedTrack,
                    navController = navController
                )
            } else {
                LaunchedEffect(
                    Unit
                ) {
                    navController.popBackStack()
                }
            }
        }

        composable(
            Screen.FolderPickerSettings.route
        ) {
            FolderPickerSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.LibraryCleanupSettings.route
        ) {
            LibraryCleanupSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.NavidromeDiagnostics.route
        ) {
            NavidromeDiagnosticsScreen(
                navController = navController
            )
        }

        composable(
            Screen.NavidromeMenu.route
        ) {
            NavidromeMenuScreen(
                navController = navController
            )
        }

        composable(
            Screen.LibraryMenu.route
        ) {
            LibraryMenuScreen(
                navController = navController
            )
        }

        composable(
            Screen.AudioMenu.route
        ) {
            AudioMenuScreen(
                navController = navController
            )
        }

        composable(
            Screen.AudioAdvancedMenu.route
        ) {
            AdvancedAudioMenuScreen(
                navController = navController
            )
        }

        composable(
            Screen.AudioInfoSettings.route
        ) {
            AudioInfoSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.AudioFocusSettings.route
        ) {
            AudioFocusSettingsScreen(
                navController = navController
            )
        }

        composable(
            Screen.DirectVolumeControlSettings.route
        ) {
            DirectVolumeControlSettingsScreen(
                navController = navController
            )
        }
    }
}