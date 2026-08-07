package com.example.auralarc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.auralarc.navigation.Screen
import com.example.auralarc.player.PlayerManager
import com.example.auralarc.ui.theme.AuralArcStyle

@Composable
fun PersistentMiniPlayerCard(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    val title =
        PlayerManager.currentTitle.value

    if (
        title.isBlank()
    ) {
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            )
            .auralArcClickable {
                navController.navigate(
                    Screen.NowPlaying.route
                )
            },
        shape = AuralArcStyle.CardShape,
        backgroundColor = AuralArcStyle.SurfaceBright,
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(
                albumArtPath = PlayerManager.currentAlbumArtPath.value,
                size = 48.dp
            )

            Spacer(
                modifier = Modifier.width(
                    12.dp
                )
            )

            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = AuralArcStyle.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = PlayerManager.currentArtist.value.ifBlank {
                        "Unknown Artist"
                    },
                    style = MaterialTheme.typography.body2,
                    color = AuralArcStyle.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AuralArcIconButton(
                onClick = {
                    navController.navigate(
                        Screen.Queue.route
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = AuralArcStyle.TextPrimary
                )
            }

            AuralArcIconButton(
                onClick = {
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        PlayerManager.pause()
                    } else {
                        PlayerManager.resume()
                    }
                }
            ) {
                Icon(
                    imageVector =
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription =
                    if (
                        PlayerManager.isPlaying.value
                    ) {
                        "Pause"
                    } else {
                        "Play"
                    },
                    tint = AuralArcStyle.TextPrimary
                )
            }
        }
    }
}