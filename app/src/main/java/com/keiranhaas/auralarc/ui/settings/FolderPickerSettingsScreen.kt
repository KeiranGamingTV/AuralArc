package com.keiranhaas.auralarc.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.storage.PickedFolderStore
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun FolderPickerSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var folders by remember {
        mutableStateOf(
            PickedFolderStore.getFolderUris(
                context
            )
        )
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (
                result.resultCode != Activity.RESULT_OK
            ) {
                return@rememberLauncherForActivityResult
            }

            val uri =
                result.data?.data
                    ?: return@rememberLauncherForActivityResult

            persistReadPermissionIfPossible(
                context = context,
                uri = uri,
                resultIntent = result.data
            )

            PickedFolderStore.addFolderUri(
                context,
                uri
            )

            folders =
                PickedFolderStore.getFolderUris(
                    context
                )
        }

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
                        text = "Folder Picker",
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
            AuralArcMessageCard(
                title = "Picked Music Folders",
                message = "Choose folders that AuralArc should scan using Android’s folder picker. AuralArc will also search these folders for matching LRC and DLRC lyric files. This works best for SD cards, USB drives, and folders Android exposes through the picker. After adding or removing folders, refresh the library."
            )

            AuralArcButton(
                onClick = {
                    launcher.launch(
                        createReadOnlyFolderPickerIntent()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp
                    )
            ) {
                Text(
                    text = "Pick Folder or External Drive"
                )
            }

            AuralArcMessageCard(
                title = "External Drive Note",
                message = "AuralArc requests read access only. NTFS, exFAT, and other drive formats depend on your phone being able to mount and expose the drive through Android’s folder picker."
            )

            if (
                folders.isEmpty()
            ) {
                AuralArcMessageCard(
                    title = "No Picked Folders",
                    message = "AuralArc will still scan your normal Music/AuralArc folders, but no extra folders have been picked yet."
                )
            } else {
                folders.forEach { folderUri ->
                    FolderUriCard(
                        folderUri = folderUri,
                        onRemove = {
                            PickedFolderStore.removeFolderUri(
                                context,
                                folderUri
                            )

                            folders =
                                PickedFolderStore.getFolderUris(
                                    context
                                )
                        }
                    )
                }
            }
        }
    }
}

private fun createReadOnlyFolderPickerIntent(): Intent {
    return Intent(
        Intent.ACTION_OPEN_DOCUMENT_TREE
    ).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        addFlags(
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        addFlags(
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
    }
}

private fun persistReadPermissionIfPossible(
    context: android.content.Context,
    uri: Uri,
    resultIntent: Intent?
) {
    val readFlag =
        Intent.FLAG_GRANT_READ_URI_PERMISSION

    val persistableFlag =
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

    val flags =
        resultIntent?.flags
            ?: 0

    val canPersistRead =
        flags and readFlag != 0 &&
                flags and persistableFlag != 0

    if (
        !canPersistRead
    ) {
        return
    }

    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            readFlag
        )
    } catch (_: Exception) {
    }
}

@Composable
private fun FolderUriCard(
    folderUri: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                14.dp
            )
        ) {
            Text(
                text = "Folder",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = folderUri,
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    top = 6.dp
                )
            )

            TextButton(
                onClick = onRemove
            ) {
                Text(
                    text = "Remove"
                )
            }
        }
    }
}