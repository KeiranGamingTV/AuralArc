package com.example.auralarc.ui

import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import android.content.Context
import android.content.pm.PackageManager
import androidx.media3.common.util.UnstableApi
import com.example.auralarc.R
import com.example.auralarc.navigation.Screen
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.storage.AppearancePreferences
import com.example.auralarc.storage.AudioBehaviorPreferences
import com.example.auralarc.storage.FolderManager
import com.example.auralarc.ui.theme.AuralArcStyle
import com.example.auralarc.player.SmartShuffleState
import com.example.auralarc.storage.TodaysPicksPreferences
import kotlin.math.roundToInt
import android.os.Build
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import com.example.auralarc.storage.LyricsPreferences
import android.app.Activity
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun SettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var duetLyricsEnabled by remember {
        mutableStateOf(
            LyricsPreferences.getDuetLyricsEnabled(
                context
            )
        )
    }

    SettingsMenuScaffold(
        title = "Settings",
        navController = navController
    ) {
        SettingsToggleCard(
            title = "Enable Duet Lyrics",
            subtitle = "Use matching .dlrc files with separate synchronized lyrics for two singers.",
            checked = duetLyricsEnabled,
            onCheckedChange = { enabled ->
                duetLyricsEnabled =
                    enabled

                LyricsPreferences.setDuetLyricsEnabled(
                    context = context,
                    enabled = enabled
                )

                LyricsState.clearCache()
            }
        )

        SettingsIconRow(
            title = "Navidrome",
            icon = Icons.Default.Cloud,
            onClick = {
                navController.navigate(
                    Screen.NavidromeMenu.route
                )
            }
        )

        SettingsIconRow(
            title = "Library",
            icon = Icons.Default.LibraryMusic,
            onClick = {
                navController.navigate(
                    Screen.LibraryMenu.route
                )
            }
        )

        SettingsIconRow(
            title = "Appearance",
            icon = Icons.Default.Palette,
            onClick = {
                navController.navigate(
                    Screen.AppearanceSettings.route
                )
            }
        )

        SettingsIconRow(
            title = "Audio",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = {
                navController.navigate(
                    Screen.AudioAdvancedMenu.route
                )
            }
        )

        SettingsIconRow(
            title = "About AuralArc",
            icon = Icons.Default.Info,
            onClick = {
                navController.navigate(
                    Screen.AboutSettings.route
                )
            }
        )
    }
}

@Composable
fun NavidromeSettingsScreen(
    navController: NavHostController
) {
    BasicSettingsSubScreen(
        title = "Navidrome Settings",
        navController = navController
    ) {
        NavidromeSettingsCard()
    }
}

@Composable
fun LibraryFolderSettingsScreen(
    navController: NavHostController
) {
    BasicSettingsSubScreen(
        title = "Library Folders",
        navController = navController
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            shape = AuralArcStyle.CardShape,
            backgroundColor = AuralArcStyle.Surface,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    14.dp
                )
            ) {
                Text(
                    text = "Local Library Folders",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = "AuralArc currently scans these local folders.",
                    style = MaterialTheme.typography.body2,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 6.dp
                    )
                )

                FolderManager.allowedFolders.forEach { folder ->
                    Text(
                        text = folder,
                        style = MaterialTheme.typography.body2,
                        color = AuralArcStyle.TextSecondary,
                        modifier = Modifier.padding(
                            top = 8.dp
                        )
                    )
                }

                Button(
                    onClick = {
                        FolderManager.folders =
                            FolderManager.allowedFolders.toMutableList()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 14.dp
                        ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AuralArcStyle.Surface,
                        contentColor = AuralArcStyle.TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = AuralArcStyle.TextPrimary
                    )

                    Spacer(
                        modifier = Modifier.width(
                            6.dp
                        )
                    )

                    Text(
                        text = "Reset Local Folders",
                        color = AuralArcStyle.TextPrimary
                    )
                }

                Text(
                    text = "After changing folder settings, refresh the library from the main screen.",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 8.dp
                    )
                )
            }
        }
    }
}

@Composable
fun AppearanceSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var compactRows by remember {
        mutableStateOf(
            AppearancePreferences.getCompactRows(
                context
            )
        )
    }

    var showArtwork by remember {
        mutableStateOf(
            AppearancePreferences.getShowArtwork(
                context
            )
        )
    }

    BasicSettingsSubScreen(
        title = "Appearance",
        navController = navController
    ) {
        SettingsToggleCard(
            title = "Compact Song Rows",
            subtitle = "Use shorter song rows in library lists.",
            checked = compactRows,
            onCheckedChange = { enabled ->
                compactRows =
                    enabled

                AppearancePreferences.setCompactRows(
                    context,
                    enabled
                )
            }
        )

        SettingsToggleCard(
            title = "Show Album Art in Lists",
            subtitle = "Show artwork thumbnails in song rows.",
            checked = showArtwork,
            onCheckedChange = { enabled ->
                showArtwork =
                    enabled

                AppearancePreferences.setShowArtwork(
                    context,
                    enabled
                )
            }
        )
    }
}

@UnstableApi
@Composable
fun AudioBehaviorSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var stopWhenClosed by remember {
        mutableStateOf(
            AudioBehaviorPreferences.getStopWhenAppClosed(
                context
            )
        )
    }

    var pauseOnDisconnect by remember {
        mutableStateOf(
            AudioBehaviorPreferences.getPauseOnHeadphoneDisconnect(
                context
            )
        )
    }

    var useAudioFocus by remember {
        mutableStateOf(
            AudioBehaviorPreferences.getUseAudioFocus(
                context
            )
        )
    }

    var smartShuffleLevel by remember {
        mutableStateOf(
            AudioBehaviorPreferences.getSmartShuffleLevel(
                context
            )
        )
    }

    BasicSettingsSubScreen(
        title = "Audio Behavior",
        navController = navController
    ) {
        SettingsToggleCard(
            title = "Stop Music When App Closes",
            subtitle = "When AuralArc is closed completely, stop playback and stop the background service.",
            checked = stopWhenClosed,
            onCheckedChange = { enabled ->
                stopWhenClosed =
                    enabled

                AudioBehaviorPreferences.setStopWhenAppClosed(
                    context,
                    enabled
                )
            }
        )

        SettingsToggleCard(
            title = "Pause on Headphone Disconnect",
            subtitle = "Pause playback when headphones, aux, or Bluetooth audio disconnects.",
            checked = pauseOnDisconnect,
            onCheckedChange = { enabled ->
                pauseOnDisconnect =
                    enabled

                AudioBehaviorPreferences.setPauseOnHeadphoneDisconnect(
                    context,
                    enabled
                )

                PlayerManager.applyAudioBehaviorPreferences(
                    context
                )
            }
        )

        SettingsToggleCard(
            title = "Use Android Audio Focus",
            subtitle = "Let Android pause or lower playback when another app needs audio.",
            checked = useAudioFocus,
            onCheckedChange = { enabled ->
                useAudioFocus =
                    enabled

                AudioBehaviorPreferences.setUseAudioFocus(
                    context,
                    enabled
                )

                PlayerManager.applyAudioBehaviorPreferences(
                    context
                )
            }
        )

        SmartShuffleSliderCard(
            level = smartShuffleLevel,
            onLevelChange = { newLevel ->
                smartShuffleLevel =
                    newLevel

                AudioBehaviorPreferences.setSmartShuffleLevel(
                    context,
                    newLevel
                )

                SmartShuffleState.level.value =
                    newLevel
            }
        )
    }
}

@Composable
fun AboutSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val appVersion =
        remember {
            getAppVersionName(
                context
            )
        }

    val sourceCodeUrl =
        "https://github.com/KeiranGamingTV/AuralArc"

    var releaseNotesExpanded by remember {
        mutableStateOf(
            false
        )
    }

    val releaseNotes =
        listOf(
            "Fixed a bug where some artists' albums would be completely empty.",
            "Fixed a bug where your Navidrome password would still show up on your keyboard while typing."
        )

    SettingsMenuScaffold(
        title = "About AuralArc",
        navController = navController
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AuralArc",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(
                    18.dp
                )
            )

            AndroidView(
                modifier = Modifier.size(
                    132.dp
                ),
                factory = { viewContext ->
                    val cornerRadiusPx =
                        34f * viewContext.resources.displayMetrics.density

                    ImageView(
                        viewContext
                    ).apply {
                        setImageResource(
                            R.mipmap.ic_launcher
                        )

                        scaleType =
                            ImageView.ScaleType.CENTER_CROP

                        clipToOutline =
                            true

                        outlineProvider =
                            object : ViewOutlineProvider() {
                                override fun getOutline(
                                    view: View,
                                    outline: Outline
                                ) {
                                    outline.setRoundRect(
                                        0,
                                        0,
                                        view.width,
                                        view.height,
                                        cornerRadiusPx
                                    )
                                }
                            }
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(
                    26.dp
                )
            )

            Text(
                text = "Version $appVersion",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(
                    16.dp
                )
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AuralArcStyle.CardShape,
                backgroundColor = AuralArcStyle.Surface,
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .auralArcClickable {
                                releaseNotesExpanded =
                                    !releaseNotesExpanded
                            }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View Release Notes",
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.Bold,
                            color = AuralArcStyle.TextPrimary,
                            modifier = Modifier.weight(
                                1f
                            )
                        )

                        Icon(
                            imageVector =
                            if (
                                releaseNotesExpanded
                            ) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription =
                            if (
                                releaseNotesExpanded
                            ) {
                                "Collapse release notes"
                            } else {
                                "Expand release notes"
                            },
                            tint = AuralArcStyle.TextSecondary
                        )
                    }

                    if (
                        releaseNotesExpanded
                    ) {
                        Divider(
                            color = AuralArcStyle.TextMuted.copy(
                                alpha = 0.25f
                            )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 18.dp,
                                    end = 18.dp,
                                    top = 14.dp,
                                    bottom = 16.dp
                                )
                        ) {
                            Text(
                                text = "--- Release Notes ---",
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AuralArcStyle.TextPrimary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    12.dp
                                )
                            )

                            releaseNotes.forEach { note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 3.dp
                                        ),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.body1,
                                        fontWeight = FontWeight.Bold,
                                        color = AuralArcStyle.PurpleBright
                                    )

                                    Spacer(
                                        modifier = Modifier.width(
                                            9.dp
                                        )
                                    )

                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.body1,
                                        color = AuralArcStyle.TextSecondary,
                                        modifier = Modifier.weight(
                                            1f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(
                    30.dp
                )
            )

            Box(
                modifier = Modifier
                    .size(
                        72.dp
                    )
                    .auralArcClickable {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    sourceCodeUrl
                                )
                            )

                        context.startActivity(
                            intent
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_github
                    ),
                    contentDescription = "Open GitHub source code",
                    tint = AuralArcStyle.TextPrimary,
                    modifier = Modifier.size(
                        46.dp
                    )
                )
            }
        }
    }
}

@Composable
fun NavidromeMenuScreen(
    navController: NavHostController
) {
    SettingsMenuScaffold(
        title = "Navidrome",
        navController = navController
    ) {
        SettingsIconRow(
            title = "Server Settings",
            icon = Icons.Default.Settings,
            onClick = {
                navController.navigate(
                    Screen.NavidromeSettings.route
                )
            }
        )

        SettingsIconRow(
            title = "Diagnostics",
            icon = Icons.Default.BugReport,
            onClick = {
                navController.navigate(
                    Screen.NavidromeDiagnostics.route
                )
            }
        )
    }
}

@Composable
fun LibraryMenuScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var todaysPicksEnabled by remember {
        mutableStateOf(
            TodaysPicksPreferences.getEnabled(
                context
            )
        )
    }

    SettingsMenuScaffold(
        title = "Library",
        navController = navController
    ) {
        SettingsToggleCard(
            title = "Today's Picks",
            subtitle = "Show or hide the Today's Picks section on the Home page.",
            checked = todaysPicksEnabled,
            onCheckedChange = { enabled ->
                todaysPicksEnabled =
                    enabled

                TodaysPicksPreferences.setEnabled(
                    context,
                    enabled
                )
            }
        )

        SettingsIconRow(
            title = "Default Folders",
            icon = Icons.Default.Folder,
            onClick = {
                navController.navigate(
                    Screen.LibraryFolderSettings.route
                )
            }
        )

        SettingsIconRow(
            title = "Clean-Up",
            icon = Icons.Default.Build,
            onClick = {
                navController.navigate(
                    Screen.LibraryCleanupSettings.route
                )
            }
        )

        SettingsIconRow(
            title = "Folder Picker",
            icon = Icons.Default.CreateNewFolder,
            onClick = {
                navController.navigate(
                    Screen.FolderPickerSettings.route
                )
            }
        )
    }
}

@Composable
fun AudioMenuScreen(
    navController: NavHostController
) {
    SettingsMenuScaffold(
        title = "Audio",
        navController = navController
    ) {
        SettingsIconRow(
            title = "Audio Behavior",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = {
                navController.navigate(
                    Screen.AudioBehaviorSettings.route
                )
            }
        )

        SettingsIconRow(
            title = "Advanced Audio",
            icon = Icons.Default.GraphicEq,
            onClick = {
                navController.navigate(
                    Screen.AudioAdvancedMenu.route
                )
            }
        )
    }
}

@Composable
private fun SmartShuffleSliderCard(
    level: Int,
    onLevelChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text = "Smart Shuffle",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = smartShuffleDescription(
                    level
                ),
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    top = 3.dp,
                    bottom = 8.dp
                )
            )

            Slider(
                value = level.toFloat(),
                onValueChange = { value ->
                    onLevelChange(
                        value.roundToInt().coerceIn(
                            0,
                            4
                        )
                    )
                },
                valueRange = 0f..4f,
                steps = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Random",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )

                Text(
                    text = "Balanced",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )

                Text(
                    text = "Least Played",
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted
                )
            }
        }
    }
}

private fun smartShuffleDescription(
    level: Int
): String {
    return when (
        level.coerceIn(
            0,
            4
        )
    ) {
        0 ->
            "Completely random shuffle."

        1 ->
            "Mostly random with a small least-played boost."

        2 ->
            "Balanced between random and least-played songs."

        3 ->
            "Strongly favors songs you have listened to less."

        else ->
            "Mostly least-played songs, with a little randomness."
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 3.dp
                    )
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingsChoiceCard(
    title: String,
    subtitle: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    top = 3.dp,
                    bottom = 8.dp
                )
            )

            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .auralArcClickable {
                            onSelected(
                                option.first
                            )
                        }
                        .padding(
                            vertical = 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedValue == option.first,
                        onClick = {
                            onSelected(
                                option.first
                            )
                        }
                    )

                    Text(
                        text = option.second,
                        color = AuralArcStyle.TextPrimary,
                        modifier = Modifier.padding(
                            start = 6.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicSettingsSubScreen(
    title: String,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    Scaffold(
        backgroundColor = AuralArcStyle.BackgroundBottom,
        topBar = {
            TopAppBar(
                backgroundColor = AuralArcStyle.BackgroundTop,
                elevation = 0.dp,
                navigationIcon = {
                    AuralArcIconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = title,
                        color = AuralArcStyle.TextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .background(
                    brush = AuralArcStyle.appBackgroundBrush()
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    bottom = 18.dp
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsMenuScaffold(
    title: String,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    Scaffold(
        backgroundColor = AuralArcStyle.BackgroundBottom,
        topBar = {
            TopAppBar(
                backgroundColor = AuralArcStyle.BackgroundTop,
                elevation = 0.dp,
                navigationIcon = {
                    AuralArcIconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuralArcStyle.TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = title,
                        color = AuralArcStyle.TextPrimary
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    innerPadding
                )
                .background(
                    brush = AuralArcStyle.appBackgroundBrush()
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    bottom = 18.dp
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsIconRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            )
            .auralArcClickable {
                onClick()
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AuralArcStyle.TextSecondary
            )

            Spacer(
                modifier = Modifier.width(
                    14.dp
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )
        }
    }
}

private fun getAppVersionName(
    context: Context
): String {
    return try {
        val packageInfo =
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(
                        0
                    )
                )
            } else {
                @Suppress(
                    "DEPRECATION"
                )
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
            }

        packageInfo.versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }
}