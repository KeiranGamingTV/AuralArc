package com.keiranhaas.auralarc.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.keiranhaas.auralarc.ui.AuralArcButton
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcContentTransition
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

private enum class FirstRunSetupStep {
    WELCOME,
    MEDIA,
    NOTIFICATIONS,
    COMPLETE
}

@Composable
fun FirstRunSetupScreen(
    onFinished: () -> Unit
) {
    val context =
        LocalContext.current

    var currentStepName by rememberSaveable {
        mutableStateOf(
            FirstRunSetupStep.WELCOME.name
        )
    }

    val currentStep =
        FirstRunSetupStep.valueOf(
            currentStepName
        )

    fun moveTo(
        step: FirstRunSetupStep
    ) {
        currentStepName =
            step.name
    }

    val mediaPermission =
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val mediaPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            moveTo(
                FirstRunSetupStep.NOTIFICATIONS
            )
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            moveTo(
                FirstRunSetupStep.COMPLETE
            )
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = AuralArcStyle.appBackgroundBrush()
            )
            .padding(
                horizontal = 22.dp,
                vertical = 28.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AuralArc",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                color = AuralArcStyle.TextPrimary
            )

            Spacer(
                modifier = Modifier.height(
                    14.dp
                )
            )

            SetupProgressIndicator(
                currentStep = currentStep
            )

            Spacer(
                modifier = Modifier.height(
                    24.dp
                )
            )

            AuralArcCard(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = AuralArcStyle.CardShape,
                backgroundColor = AuralArcStyle.SurfaceBright,
                elevation = 10.dp
            ) {
                AuralArcContentTransition(
                    targetState = currentStep
                ) { step ->
                    when (
                        step
                    ) {
                        FirstRunSetupStep.WELCOME -> {
                            SetupStepContent(
                                icon = Icons.Default.MusicNote,
                                title = "Welcome to AuralArc",
                                message = "AuralArc brings your local music and Navidrome library together in one focused music player.",
                                buttonText = "Get Started",
                                onButtonClick = {
                                    moveTo(
                                        FirstRunSetupStep.MEDIA
                                    )
                                }
                            )
                        }

                        FirstRunSetupStep.MEDIA -> {
                            SetupStepContent(
                                icon = Icons.Default.LibraryMusic,
                                title = "Media Access",
                                message = "Allow AuralArc to read audio on this device so it can find, organize, and play your local music library.",
                                buttonText = "Grant Media Access",
                                onButtonClick = {
                                    val alreadyGranted =
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            mediaPermission
                                        ) == PackageManager.PERMISSION_GRANTED

                                    if (
                                        alreadyGranted
                                    ) {
                                        moveTo(
                                            FirstRunSetupStep.NOTIFICATIONS
                                        )
                                    } else {
                                        mediaPermissionLauncher.launch(
                                            mediaPermission
                                        )
                                    }
                                }
                            )
                        }

                        FirstRunSetupStep.NOTIFICATIONS -> {
                            SetupStepContent(
                                icon = Icons.Default.Notifications,
                                title = "Playback Notifications",
                                message = "Allow notifications so AuralArc can show playback controls and the current song while music is playing.",
                                buttonText =
                                    if (
                                        Build.VERSION.SDK_INT >=
                                        Build.VERSION_CODES.TIRAMISU
                                    ) {
                                        "Grant Notification Access"
                                    } else {
                                        "Continue"
                                    },
                                onButtonClick = {
                                    if (
                                        Build.VERSION.SDK_INT <
                                        Build.VERSION_CODES.TIRAMISU
                                    ) {
                                        moveTo(
                                            FirstRunSetupStep.COMPLETE
                                        )
                                    } else {
                                        val alreadyGranted =
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED

                                        if (
                                            alreadyGranted
                                        ) {
                                            moveTo(
                                                FirstRunSetupStep.COMPLETE
                                            )
                                        } else {
                                            notificationPermissionLauncher.launch(
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        FirstRunSetupStep.COMPLETE -> {
                            SetupStepContent(
                                icon = Icons.Default.CheckCircle,
                                title = "You're All Set",
                                message = "AuralArc is ready. You can change library, notification, playback, and appearance settings at any time from Settings.",
                                buttonText = "Open AuralArc",
                                onButtonClick = onFinished
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepContent(
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuralArcCard(
            modifier = Modifier.size(
                82.dp
            ),
            shape = CircleShape,
            backgroundColor = AuralArcStyle.PurpleDark,
            elevation = 6.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AuralArcStyle.TextPrimary,
                    modifier = Modifier.size(
                        42.dp
                    )
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                22.dp
            )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = AuralArcStyle.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(
                10.dp
            )
        )

        Text(
            text = message,
            style = MaterialTheme.typography.body1,
            color = AuralArcStyle.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(
                26.dp
            )
        )

        AuralArcButton(
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    52.dp
                ),
            shape = AuralArcStyle.SmallShape,
            containerColor =
                AuralArcStyle.Purple,
            contentColor =
                AuralArcStyle.TextPrimary
        ) {
            Text(
                text = buttonText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SetupProgressIndicator(
    currentStep: FirstRunSetupStep
) {
    val currentIndex =
        currentStep.ordinal

    Row(
        horizontalArrangement = Arrangement.spacedBy(
            8.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FirstRunSetupStep.entries.forEachIndexed { index, _ ->
            Box(
                modifier = Modifier
                    .size(
                        if (
                            index == currentIndex
                        ) {
                            10.dp
                        } else {
                            8.dp
                        }
                    )
                    .background(
                        color =
                            if (
                                index <= currentIndex
                            ) {
                                AuralArcStyle.PurpleBright
                            } else {
                                AuralArcStyle.Divider
                            },
                        shape = CircleShape
                    )
            )
        }
    }
}