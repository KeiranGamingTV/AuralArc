package com.keiranhaas.auralarc.ui.driving

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keiranhaas.auralarc.ui.AuralArcButton
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

/**
 * AuralArc's own Bluetooth device picker. It is drawn on top of the
 * screen (not an Android dialog) and lists the phone's paired devices.
 */
@Composable
fun BluetoothDevicePicker(
    state: BluetoothPickerState,
    devices: List<PairedBluetoothDevice>,
    selectedAddress: String?,
    onSelect: (PairedBluetoothDevice) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context =
        LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(
                    alpha = 0.65f
                )
            )
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        AuralArcCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp
                )
                // Taps inside the card must not close the picker.
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClick = {}
                ),
            backgroundColor = AuralArcStyle.Surface,
            elevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    16.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(
                            1f
                        )
                    ) {
                        Text(
                            text = "Choose a device",
                            color = AuralArcStyle.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Driving Mode opens when this device connects.",
                            color = AuralArcStyle.TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(
                                top = 2.dp
                            )
                        )
                    }

                    if (
                        state == BluetoothPickerState.READY
                    ) {
                        IconButton(
                            onClick = onRefresh
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh devices",
                                tint = AuralArcStyle.TextSecondary
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.size(
                        10.dp
                    )
                )

                when (state) {
                    BluetoothPickerState.NEEDS_PERMISSION -> {
                        PickerMessage(
                            text = "AuralArc needs the “Nearby devices” permission to see your paired Bluetooth devices."
                        )

                        AuralArcButton(
                            onClick = onRequestPermission,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 10.dp
                                )
                        ) {
                            Text(
                                text = "Allow Nearby Devices"
                            )
                        }
                    }

                    BluetoothPickerState.BLUETOOTH_OFF -> {
                        PickerMessage(
                            text = "Bluetooth is turned off. Turn it on, then tap refresh."
                        )

                        AuralArcButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_BLUETOOTH_SETTINGS
                                    ).addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 10.dp
                                )
                        ) {
                            Text(
                                text = "Open Bluetooth Settings"
                            )
                        }
                    }

                    BluetoothPickerState.NOT_SUPPORTED -> {
                        PickerMessage(
                            text = "This device doesn’t support Bluetooth."
                        )
                    }

                    BluetoothPickerState.READY -> {
                        if (
                            devices.isEmpty()
                        ) {
                            PickerMessage(
                                text = "No paired devices found. Pair your car or headphones in Android’s Bluetooth settings first."
                            )

                            AuralArcButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_BLUETOOTH_SETTINGS
                                        ).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 10.dp
                                    )
                            ) {
                                Text(
                                    text = "Open Bluetooth Settings"
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(
                                    max = 340.dp
                                ),
                                verticalArrangement =
                                Arrangement.spacedBy(
                                    6.dp
                                )
                            ) {
                                items(
                                    devices,
                                    key = { device ->
                                        device.address
                                    }
                                ) { device ->
                                    BluetoothDeviceRow(
                                        device = device,
                                        selected =
                                        device.address == selectedAddress,
                                        onClick = {
                                            onSelect(
                                                device
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(
                            Alignment.End
                        )
                        .padding(
                            top = 8.dp
                        )
                ) {
                    Text(
                        text = "Cancel",
                        color = AuralArcStyle.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerMessage(
    text: String
) {
    Text(
        text = text,
        color = AuralArcStyle.TextSecondary,
        fontSize = 15.sp
    )
}

@Composable
private fun BluetoothDeviceRow(
    device: PairedBluetoothDevice,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector =
        when (device.kind) {
            BluetoothDeviceKind.CAR ->
                Icons.Filled.DirectionsCar

            BluetoothDeviceKind.AUDIO ->
                Icons.Filled.Headphones

            BluetoothDeviceKind.OTHER ->
                Icons.Filled.Bluetooth
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    14.dp
                )
            )
            .background(
                if (
                    selected
                ) {
                    AuralArcStyle.Purple.copy(
                        alpha = 0.28f
                    )
                } else {
                    AuralArcStyle.SurfaceSoft
                }
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (
                selected
            ) {
                AuralArcStyle.PurpleBright
            } else {
                AuralArcStyle.TextSecondary
            }
        )

        Spacer(
            modifier = Modifier.width(
                14.dp
            )
        )

        Column(
            modifier = Modifier.weight(
                1f
            )
        ) {
            Text(
                text = device.name,
                color = AuralArcStyle.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = when (device.kind) {
                    BluetoothDeviceKind.CAR ->
                        "Car audio · ${device.address}"

                    BluetoothDeviceKind.AUDIO ->
                        "Audio device · ${device.address}"

                    BluetoothDeviceKind.OTHER ->
                        device.address
                },
                color = AuralArcStyle.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (
            selected
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = AuralArcStyle.PurpleBright
            )
        }
    }
}