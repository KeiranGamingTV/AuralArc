package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.AuralArcButton
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle
import androidx.compose.material3.MaterialTheme

@Composable
fun AuralArcMessageCard(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    AuralArcCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),

        shape =
            AuralArcStyle.CardShape,

        backgroundColor =
            AuralArcStyle.SurfaceBright,

        elevation =
            8.dp
    ) {
        Column(
            modifier =
                Modifier.padding(
                    AuralArcStyle.CardPadding
                )
        ) {
            Text(
                text =
                    title,

                style =
                    MaterialTheme.typography.titleLarge,

                fontWeight =
                    FontWeight.Bold,

                color =
                    AuralArcStyle.TextPrimary
            )

            Text(
                text =
                    message,

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    AuralArcStyle.TextMuted,

                modifier =
                    Modifier.padding(
                        top = 6.dp
                    )
            )

            if (
                actionText != null &&
                onAction != null
            ) {
                AuralArcButton(
                    onClick =
                        onAction,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 12.dp
                            )
                ) {
                    Text(
                        text =
                            actionText
                    )
                }
            }
        }
    }
}

@Composable
fun AuralArcLoadingCard(
    title: String = "Loading...",
    message: String = "Please wait."
) {
    AuralArcMessageCard(
        title =
            title,

        message =
            message
    )
}