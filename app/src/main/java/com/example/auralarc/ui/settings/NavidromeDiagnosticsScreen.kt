package com.example.auralarc.ui

import android.net.Uri
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
import com.example.auralarc.navidrome.NavidromeClient
import com.example.auralarc.navidrome.NavidromePreferences
import com.example.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    var status by remember {
        mutableStateOf(
            ""
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
                        text = "Navidrome Diagnostics",
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
                    status =
                        "Testing connection..."

                    scope.launch {
                        status =
                            withContext(
                                Dispatchers.IO
                            ) {
                                try {
                                    NavidromeClient(
                                        credentials
                                    ).ping()

                                    "Connection test passed."
                                } catch (e: Exception) {
                                    "Connection test failed: ${e.message ?: "Unknown error"}"
                                }
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
                    text = "Test Connection"
                )
            }

            if (
                status.isNotBlank()
            ) {
                AuralArcMessageCard(
                    title = "Status",
                    message = status
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
    Card(
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
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            rows.forEach { row ->
                Text(
                    text = row.first,
                    style = MaterialTheme.typography.caption,
                    color = AuralArcStyle.TextMuted,
                    modifier = Modifier.padding(
                        top = 10.dp
                    )
                )

                Text(
                    text = row.second,
                    style = MaterialTheme.typography.body2,
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