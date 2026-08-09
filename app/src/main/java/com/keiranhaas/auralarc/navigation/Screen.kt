package com.keiranhaas.auralarc.navigation

sealed class Screen(
    val route: String
) {
    object Library : Screen(
        "library"
    )

    object NowPlaying : Screen(
        "now_playing"
    )

    object Queue : Screen(
        "queue"
    )

    object Settings : Screen(
        "settings"
    )

    object NavidromeSettings : Screen(
        "settings_navidrome"
    )

    object LibraryFolderSettings : Screen(
        "settings_library_folders"
    )

    object ListeningStats : Screen(
        "settings_listening_stats"
    )

    object AppearanceSettings : Screen(
        "settings_appearance"
    )

    object AudioBehaviorSettings : Screen(
        "settings_audio_behavior"
    )

    object AboutSettings : Screen(
        "settings_about"
    )

    object TrackInfo : Screen(
        "track_info"
    )

    object FolderPickerSettings : Screen(
        "settings_folder_picker"
    )

    object LibraryCleanupSettings : Screen(
        "settings_library_cleanup"
    )

    object NavidromeDiagnostics : Screen(
        "settings_navidrome_diagnostics"
    )

    object NavidromeMenu : Screen(
        "settings_navidrome_menu"
    )

    object LibraryMenu : Screen(
        "settings_library_menu"
    )

    object AudioMenu : Screen(
        "settings_audio_menu"
    )

    object AudioAdvancedMenu : Screen(
        "audio_advanced_menu"
    )

    object AudioInfoSettings : Screen(
        "audio_info_settings"
    )

    object AudioFocusSettings : Screen(
        "audio_focus_settings"
    )

    object DirectVolumeControlSettings : Screen(
        "direct_volume_control_settings"
    )
}