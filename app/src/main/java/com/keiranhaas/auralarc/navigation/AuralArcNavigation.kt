package com.keiranhaas.auralarc.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.keiranhaas.auralarc.storage.AppearancePreferences
import com.keiranhaas.auralarc.storage.DrivingModePreferences
import com.keiranhaas.auralarc.ui.AboutSettingsScreen
import com.keiranhaas.auralarc.ui.AdvancedAudioMenuScreen
import com.keiranhaas.auralarc.ui.AppearanceSettingsScreen
import com.keiranhaas.auralarc.ui.AudioBehaviorSettingsScreen
import com.keiranhaas.auralarc.ui.AudioFocusSettingsScreen
import com.keiranhaas.auralarc.ui.AudioInfoSettingsScreen
import com.keiranhaas.auralarc.ui.AudioMenuScreen
import com.keiranhaas.auralarc.ui.CrossfadeSettingsScreen
import com.keiranhaas.auralarc.ui.DirectVolumeControlSettingsScreen
import com.keiranhaas.auralarc.ui.DrivingModeSettingsScreen
import com.keiranhaas.auralarc.ui.EqualizerSettingsScreen
import com.keiranhaas.auralarc.ui.FolderPickerSettingsScreen
import com.keiranhaas.auralarc.ui.LibraryCleanupSettingsScreen
import com.keiranhaas.auralarc.ui.LibraryFolderSettingsScreen
import com.keiranhaas.auralarc.ui.LibraryMenuScreen
import com.keiranhaas.auralarc.ui.ListeningStatsScreen
import com.keiranhaas.auralarc.ui.ListeningStatsTracker
import com.keiranhaas.auralarc.ui.MusicLibraryView
import com.keiranhaas.auralarc.ui.NavidromeDiagnosticsScreen
import com.keiranhaas.auralarc.ui.NavidromeMenuScreen
import com.keiranhaas.auralarc.ui.NavidromeSettingsScreen
import com.keiranhaas.auralarc.ui.NowPlayingScreen
import com.keiranhaas.auralarc.ui.QueueScreen
import com.keiranhaas.auralarc.ui.SettingsScreen
import com.keiranhaas.auralarc.ui.SoundEffectsSettingsScreen
import com.keiranhaas.auralarc.ui.TrackInfoNavigationState
import com.keiranhaas.auralarc.ui.TrackInfoScreen
import com.keiranhaas.auralarc.ui.driving.DrivingModeScreen
import com.keiranhaas.auralarc.ui.safePopBackStack
import com.keiranhaas.auralarc.ui.theme.MiniPlayerMorphOverlay

@Composable
fun AuralArcNavigation(
    openDrivingModeRequest: Boolean = false,
    onDrivingModeRequestHandled: () -> Unit = {}
) {
    val navController =
        rememberNavController()

    val context =
        LocalContext.current

    DrivingModePreferences.initialize(context)

    val drivingModeEnabled =
        DrivingModePreferences.drivingModeEnabled

    AppearancePreferences.initializePageAnimations(
        context
    )

    val pageAnimationsEnabled by
    AppearancePreferences.pageAnimationsEnabledState

    ListeningStatsTracker()

    LaunchedEffect(openDrivingModeRequest) {
        if (openDrivingModeRequest) {
            DrivingModePreferences.setDrivingModeEnabled(
                context,
                true
            )

            onDrivingModeRequestHandled()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!drivingModeEnabled) {
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                enterTransition = {
                    auralArcEnter(
                        pageAnimationsEnabled
                    )
                },

                exitTransition = {
                    auralArcExit(
                        pageAnimationsEnabled
                    )
                },

                popEnterTransition = {
                    auralArcPopEnter(
                        pageAnimationsEnabled
                    )
                },

                popExitTransition = {
                    auralArcPopExit(
                        pageAnimationsEnabled
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
                            navController.safePopBackStack()
                        }
                    }
                }

                composable(
                    Screen.DrivingModeSettings.route
                ) {
                    DrivingModeSettingsScreen(
                        navController = navController
                    )
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

                composable(
                    Screen.EqualizerSettings.route
                ) {
                    EqualizerSettingsScreen(
                        navController = navController
                    )
                }

                composable(
                    Screen.SoundEffectsSettings.route
                ) {
                    SoundEffectsSettingsScreen(
                        navController = navController
                    )
                }

                composable(
                    Screen.CrossfadeSettings.route
                ) {
                    CrossfadeSettingsScreen(
                        navController = navController
                    )
                }
            }
        } else {
            DrivingModeScreen(
                onExit = {
                    DrivingModePreferences.setDrivingModeEnabled(
                        context,
                        false
                    )
                }
            )
        }
        MiniPlayerMorphOverlay(
            navController = navController
        )
    }
}
