package com.keiranhaas.auralarc.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun AuralArcTopBar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            androidx.compose.material3.Text(
                text =
                    title,

                style =
                    MaterialTheme.typography.titleLarge,

                color =
                    AuralArcStyle.TextPrimary
            )
        },

        navigationIcon = {
            if (
                navigationIcon != null
            ) {
                navigationIcon()
            }
        },

        actions =
            actions,

        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor =
                    Color.Transparent,

                scrolledContainerColor =
                    AuralArcStyle.Surface,

                titleContentColor =
                    AuralArcStyle.TextPrimary,

                navigationIconContentColor =
                    AuralArcStyle.TextPrimary,

                actionIconContentColor =
                    AuralArcStyle.TextPrimary
            )
    )
}

@Composable
fun AuralArcBackButton(
    onClick: () -> Unit
) {
    IconButton(
        onClick =
            onClick
    ) {
        androidx.compose.material3.Icon(
            imageVector =
                Icons.AutoMirrored.Filled.ArrowBack,

            contentDescription =
                "Back",

            tint =
                AuralArcStyle.TextPrimary
        )
    }
}