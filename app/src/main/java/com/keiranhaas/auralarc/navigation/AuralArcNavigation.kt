package com.keiranhaas.auralarc.navigation

import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.keiranhaas.auralarc.ui.*
import androidx.compose.runtime.LaunchedEffect
import com.keiranhaas.auralarc.ui.TrackInfoNavigationState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import com.keiranhaas.auralarc.ui.theme.AuralArcMotion
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.keiranhaas.auralarc.storage.AppearancePreferences

@Composable
fun AuralArcNavigation() {
    val navController =
        rememberNavController()

    val context =
        LocalContext.current

    AppearancePreferences.initializePageAnimations(
        context
    )

    val pageAnimationsEnabled by
    AppearancePreferences.pageAnimationsEnabledState

    ListeningStatsTracker()

    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        enterTransition = {
            if (
                pageAnimationsEnabled
            ) {
                scaleIn(
                    initialScale =
                        AuralArcMotion.CONTENT_ENTER_SCALE,
                    animationSpec = spring(
                        dampingRatio =
                            AuralArcMotion.DAMPING_RATIO,
                        stiffness =
                            AuralArcMotion.STIFFNESS
                    )
                ) +
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 90
                            )
                        )
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NORMAL
                    )
                )
            }
        },

        exitTransition = {
            if (
                pageAnimationsEnabled
            ) {
                scaleOut(
                    targetScale =
                        AuralArcMotion.CONTENT_EXIT_SCALE,
                    animationSpec = spring(
                        dampingRatio =
                            AuralArcMotion.DAMPING_RATIO,
                        stiffness =
                            AuralArcMotion.STIFFNESS
                    )
                ) +
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 110
                            )
                        )
            } else {
                fadeOut(
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NORMAL
                    )
                )
            }
        },

        popEnterTransition = {
            if (
                pageAnimationsEnabled
            ) {
                scaleIn(
                    initialScale =
                        AuralArcMotion.CONTENT_EXIT_SCALE,
                    animationSpec = spring(
                        dampingRatio =
                            AuralArcMotion.DAMPING_RATIO,
                        stiffness =
                            AuralArcMotion.STIFFNESS
                    )
                ) +
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 90
                            )
                        )
            } else {
                fadeIn(
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NORMAL
                    )
                )
            }
        },

        popExitTransition = {
            if (
                pageAnimationsEnabled
            ) {
                scaleOut(
                    targetScale =
                        AuralArcMotion.CONTENT_ENTER_SCALE,
                    animationSpec = spring(
                        dampingRatio =
                            AuralArcMotion.DAMPING_RATIO,
                        stiffness =
                            AuralArcMotion.STIFFNESS
                    )
                ) +
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 110
                            )
                        )
            } else {
                fadeOut(
                    animationSpec = tween(
                        durationMillis =
                            AuralArcMotion.NORMAL
                    )
                )
            }
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