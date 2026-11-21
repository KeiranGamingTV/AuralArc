package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.storage.ReleaseNotesPreferences
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

/**
 * Shows the current version's release notes once, the first time the app
 * is opened after an update. Place it once, alongside the app's root UI.
 */
@Composable
fun WhatsNewDialogHost() {
    val context =
        LocalContext.current

    val currentVersion =
        remember {
            getAppVersionName(
                context
            )
        }

    var visible by remember {
        mutableStateOf(
            ReleaseNotesPreferences.shouldShowWhatsNew(
                context,
                currentVersion
            )
        )
    }

    if (
        !visible
    ) {
        return
    }

    val entry =
        ReleaseNotes.forVersion(
            currentVersion
        )

    if (
        entry == null
    ) {
        /*
         * No notes were written for this version, so there is nothing to
         * show. Record it so we don't keep checking.
         */
        ReleaseNotesPreferences.markSeen(
            context,
            currentVersion
        )

        visible =
            false

        return
    }

    val dismiss = {
        ReleaseNotesPreferences.markSeen(
            context,
            currentVersion
        )

        visible =
            false
    }

    AlertDialog(
        onDismissRequest = dismiss,
        containerColor = AuralArcStyle.Surface,
        title = {
            Text(
                text = "What's New in AuralArc ${entry.version}",
                color = AuralArcStyle.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(
                        max = 420.dp
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                ReleaseNoteBullets(
                    notes = entry.notes
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = dismiss
            ) {
                Text(
                    text = "Got it",
                    color = AuralArcStyle.PurpleBright
                )
            }
        }
    )
}