package com.keiranhaas.auralarc.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.keiranhaas.auralarc.ui.components.AuralArcBackButton
import com.keiranhaas.auralarc.ui.components.AuralArcTopBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.navidrome.NavidromeClient
import com.keiranhaas.auralarc.navidrome.NavidromePreferences
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.keiranhaas.auralarc.navidrome.DiagnosticsSection
import com.keiranhaas.auralarc.navidrome.NavidromeDiagnostics

@Composable
fun NavidromeDiagnosticsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val credentials =
        NavidromePreferences.getCredentials(
            context
        )

    var running by remember {
        mutableStateOf(
            false
        )
    }

    var progress by remember {
        mutableStateOf(
            ""
        )
    }

    val sections =
        remember {
            mutableStateListOf<DiagnosticsSection>()
        }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            brush = AuralArcStyle.appBackgroundBrush()
        ),
        topBar = {
            AuralArcTopBar(
                title = "Navidrome Diagnostics",
                navigationIcon = {
                    AuralArcBackButton(
                        onClick = {
                            navController.safePopBackStack()
                        }
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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    bottom = 18.dp
                )
        ) {
            if (
                credentials == null
            ) {
                AuralArcMessageCard(
                    title = "No Navidrome Login Saved",
                    message = "Go to Settings > Navidrome and save your server info first."
                )

                return@Column
            }

            DiagnosticsCard(
                title = "Connection",
                rows = listOf(
                    "Server Host" to safeHost(
                        credentials.serverUrl
                    ),
                    "Username Saved" to credentials.username,
                    "Password Saved" to "Yes",
                    "Streaming Endpoint" to "stream.view",
                    "Streaming Quality" to "format=raw / original-quality request",
                    "Cover Art Size" to "300px request",
                    "Auth Logging" to "Full auth URLs should not be logged"
                )
            )

            AuralArcButton(
                onClick = {
                    if (!running) {
                        running = true
                        progress = "Starting..."
                        sections.clear()

                        scope.launch {
                            withContext(
                                Dispatchers.IO
                            ) {
                                NavidromeDiagnostics.run(
                                    context = context,
                                    credentials = credentials,
                                    onProgress = {
                                        progress = it
                                    },
                                    onSection = {
                                        sections.add(it)
                                    }
                                )
                            }

                            running = false
                            progress = ""
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 8.dp
                    )
            ) {
                Text(
                    text =
                        if (running) {
                            "Running..."
                        } else {
                            "Run Diagnostics"
                        }
                )
            }

            if (running) {
                AuralArcMessageCard(
                    title = "Running Diagnostics",
                    message = progress
                )
            }

            sections.forEach { section ->
                DiagnosticsCard(
                    title = section.title,
                    rows = section.rows
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(
    title: String,
    rows: List<Pair<String, String>>
) {
    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            rows.forEach { row ->
                Text(
                    text = row.first,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 10.dp
                    )
                )

                Text(
                    text = row.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuralArcStyle.TextPrimary
                )
            }
        }
    }
}

private fun safeHost(
    serverUrl: String
): String {
    return try {
        val uri =
            Uri.parse(
                serverUrl
            )

        uri.host ?: serverUrl
    } catch (_: Exception) {
        serverUrl
    }
}