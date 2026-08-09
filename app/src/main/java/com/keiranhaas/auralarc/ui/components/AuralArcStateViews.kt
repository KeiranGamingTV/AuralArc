package com.keiranhaas.auralarc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun AuralArcMessageCard(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
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

            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = AuralArcStyle.TextMuted,
                modifier = Modifier.padding(
                    top = 6.dp
                )
            )

            if (
                actionText != null &&
                onAction != null
            ) {
                AuralArcButton(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 12.dp
                        )
                ) {
                    Text(
                        text = actionText
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
        title = title,
        message = message
    )
}