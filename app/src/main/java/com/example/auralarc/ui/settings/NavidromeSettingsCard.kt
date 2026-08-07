package com.example.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.auralarc.navidrome.NavidromeClient
import com.example.auralarc.navidrome.NavidromeCredentials
import com.example.auralarc.navidrome.NavidromePreferences
import com.example.auralarc.ui.theme.AuralArcStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NavidromeSettingsCard() {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val savedCredentials =
        NavidromePreferences.getCredentials(
            context
        )

    var serverUrl by remember {
        mutableStateOf(
            savedCredentials?.serverUrl ?: ""
        )
    }

    var username by remember {
        mutableStateOf(
            savedCredentials?.username ?: ""
        )
    }

    var password by remember {
        mutableStateOf(
            savedCredentials?.password ?: ""
        )
    }

    var statusText by remember {
        mutableStateOf(
            if (
                savedCredentials == null
            ) {
                "Not connected"
            } else {
                "Saved server: ${savedCredentials.serverUrl}"
            }
        )
    }

    var isTesting by remember {
        mutableStateOf(false)
    }

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
            modifier = Modifier
                .padding(
                    14.dp
                )
        ) {
            Text(
                text = "Navidrome",
                style = MaterialTheme.typography.h6,
                color = AuralArcStyle.TextPrimary
            )

            Text(
                text = "Use your Tailscale URL, like http://100.x.x.x:4533. Do not add /rest.",
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted
            )

            Spacer(
                modifier = Modifier.height(
                    10.dp
                )
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl =
                        it
                },
                label = {
                    Text(
                        text = "Server URL"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(
                    8.dp
                )
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username =
                        it
                },
                label = {
                    Text(
                        text = "Username"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(
                    8.dp
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password =
                        it
                },
                label = {
                    Text(
                        text = "Password"
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(
                    10.dp
                )
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.caption,
                color = AuralArcStyle.TextSecondary
            )

            Spacer(
                modifier = Modifier.height(
                    10.dp
                )
            )

            Button(
                enabled =
                !isTesting &&
                        serverUrl.isNotBlank() &&
                        username.isNotBlank() &&
                        password.isNotBlank(),
                onClick = {
                    val credentials =
                        NavidromeCredentials(
                            serverUrl = serverUrl,
                            username = username,
                            password = password
                        )

                    NavidromePreferences.saveCredentials(
                        context,
                        credentials
                    )

                    isTesting =
                        true

                    statusText =
                        "Testing connection..."

                    scope.launch {
                        val result =
                            withContext(
                                Dispatchers.IO
                            ) {
                                NavidromeClient(
                                    credentials
                                ).ping()
                            }

                        statusText =
                            result.message

                        isTesting =
                            false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text =
                    if (
                        isTesting
                    ) {
                        "Testing..."
                    } else {
                        "Save & Test Connection"
                    }
                )
            }

            TextButton(
                onClick = {
                    NavidromePreferences.clearCredentials(
                        context
                    )

                    serverUrl =
                        ""

                    username =
                        ""

                    password =
                        ""

                    statusText =
                        "Navidrome settings cleared."
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Clear Navidrome Settings"
                )
            }
        }
    }
}