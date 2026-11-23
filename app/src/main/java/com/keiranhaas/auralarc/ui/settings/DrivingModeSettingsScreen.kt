package com.keiranhaas.auralarc.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.keiranhaas.auralarc.storage.DrivingModePreferences
import com.keiranhaas.auralarc.ui.components.AuralArcBackButton
import com.keiranhaas.auralarc.ui.components.AuralArcCard
import com.keiranhaas.auralarc.ui.components.AuralArcSwitch
import com.keiranhaas.auralarc.ui.components.AuralArcTopBar
import com.keiranhaas.auralarc.ui.driving.BluetoothDevicePicker
import com.keiranhaas.auralarc.ui.driving.BluetoothDevices
import com.keiranhaas.auralarc.ui.driving.BluetoothPickerState
import com.keiranhaas.auralarc.ui.driving.PairedBluetoothDevice
import com.keiranhaas.auralarc.ui.theme.AuralArcStyle

@Composable
fun DrivingModeSettingsScreen(
    navController: NavHostController
) {
    val context =
        LocalContext.current

    var autoEnabled by remember {
        mutableStateOf(
            DrivingModePreferences.bluetoothAutoEnabled(
                context
            )
        )
    }

    var selectedName by remember {
        mutableStateOf(
            DrivingModePreferences.selectedDeviceName(
                context
            )
        )
    }

    var selectedAddress by remember {
        mutableStateOf(
            DrivingModePreferences.selectedDeviceAddress(
                context
            )
        )
    }

    var showPicker by remember {
        mutableStateOf(
            false
        )
    }

    var pickerState by remember {
        mutableStateOf(
            BluetoothDevices.pickerState(
                context
            )
        )
    }

    var devices by remember {
        mutableStateOf(
            emptyList<PairedBluetoothDevice>()
        )
    }

    var notificationsAllowed by remember {
        mutableStateOf(
            BluetoothDevices.hasNotificationPermission(
                context
            )
        )
    }

    fun refreshDevices() {
        pickerState =
            BluetoothDevices.pickerState(
                context
            )

        devices =
            BluetoothDevices.loadPairedDevices(
                context
            )

        notificationsAllowed =
            BluetoothDevices.hasNotificationPermission(
                context
            )
    }

    var openPickerAfterPermission by remember {
        mutableStateOf(
            false
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            refreshDevices()

            if (
                openPickerAfterPermission &&
                BluetoothDevices.hasConnectPermission(
                    context
                )
            ) {
                showPicker =
                    true
            }

            openPickerAfterPermission =
                false
        }

    fun openPicker() {
        val missing =
            BluetoothDevices.missingPermissions(
                context
            )

        if (
            missing.isNotEmpty()
        ) {
            openPickerAfterPermission =
                true

            permissionLauncher.launch(
                missing
            )
        } else {
            refreshDevices()

            showPicker =
                true
        }
    }

    fun closePicker() {
        showPicker =
            false

        // No device chosen means Bluetooth activation can't work.
        if (
            selectedAddress == null
        ) {
            autoEnabled =
                false

            DrivingModePreferences.setBluetoothAutoEnabled(
                context,
                false
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AuralArcTopBar(
                    title = "Driving Mode",
                    navigationIcon = {
                        AuralArcBackButton(
                            onClick = {
                                navController.safePopBackStack()
                            }
                        )
                    }
                )
            },
            modifier = Modifier.background(
                brush = AuralArcStyle.appBackgroundBrush()
            )
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
                AuralArcMessageCard(
                    title = "Open Driving Mode automatically",
                    message = "Pick your car’s Bluetooth. When it connects, AuralArc opens Driving Mode with large, easy-to-hit controls."
                )

                SettingsCardRow(
                    title = "Open when Bluetooth connects",
                    subtitle = if (
                        autoEnabled
                    ) {
                        "On. Driving Mode opens when your chosen device connects."
                    } else {
                        "Off. Driving Mode only opens when you turn it on yourself."
                    }
                ) {
                    AuralArcSwitch(
                        checked = autoEnabled,
                        onCheckedChange = { enabled ->
                            autoEnabled =
                                enabled

                            DrivingModePreferences.setBluetoothAutoEnabled(
                                context,
                                enabled
                            )

                            if (
                                enabled
                            ) {
                                val missing =
                                    BluetoothDevices.missingPermissions(
                                        context
                                    )

                                if (
                                    selectedAddress == null
                                ) {
                                    openPicker()
                                } else if (
                                    missing.isNotEmpty()
                                ) {
                                    permissionLauncher.launch(
                                        missing
                                    )
                                }
                            }
                        }
                    )
                }

                AuralArcCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 7.dp
                        ),
                    backgroundColor = AuralArcStyle.Surface,
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(
                            14.dp
                        )
                    ) {
                        Text(
                            text = "Bluetooth device",
                            color = AuralArcStyle.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = selectedName
                                ?: "No device chosen yet",
                            color = if (
                                selectedAddress == null
                            ) {
                                AuralArcStyle.TextMuted
                            } else {
                                AuralArcStyle.TextSecondary
                            },
                            fontSize = 15.sp,
                            modifier = Modifier.padding(
                                top = 4.dp
                            )
                        )

                        if (
                            selectedAddress != null
                        ) {
                            Text(
                                text = selectedAddress.orEmpty(),
                                color = AuralArcStyle.TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 12.dp
                                ),
                            horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            ),
                            verticalAlignment =
                            Alignment.CenterVertically
                        ) {
                            AuralArcButton(
                                onClick = {
                                    openPicker()
                                },
                                modifier = Modifier.weight(
                                    1f
                                )
                            ) {
                                Text(
                                    text = if (
                                        selectedAddress == null
                                    ) {
                                        "Choose Device"
                                    } else {
                                        "Change Device"
                                    }
                                )
                            }

                            if (
                                selectedAddress != null
                            ) {
                                AuralArcButton(
                                    onClick = {
                                        DrivingModePreferences.setSelectedDevice(
                                            context,
                                            null,
                                            null
                                        )

                                        DrivingModePreferences.setBluetoothAutoEnabled(
                                            context,
                                            false
                                        )

                                        selectedAddress =
                                            null

                                        selectedName =
                                            null

                                        autoEnabled =
                                            false
                                    },
                                    containerColor =
                                    AuralArcStyle.SurfaceSoft
                                ) {
                                    Text(
                                        text = "Clear"
                                    )
                                }
                            }
                        }
                    }
                }

                if (
                    autoEnabled &&
                    !notificationsAllowed
                ) {
                    AuralArcMessageCard(
                        title = "Notifications are off",
                        message = "Android sometimes blocks apps from opening in the background. Allow notifications so AuralArc can show a “Driving Mode ready” tap-to-open alert as a backup.",
                        actionText = "Allow Notifications",
                        onAction = {
                            permissionLauncher.launch(
                                BluetoothDevices.missingPermissions(
                                    context
                                ).filter { permission ->
                                    permission ==
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                }.toTypedArray()
                            )
                        }
                    )
                }

                AuralArcButton(
                    onClick = {
                        DrivingModePreferences.setDrivingModeEnabled(
                            context,
                            true
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        )
                ) {
                    Text(
                        text = "Open Driving Mode Now"
                    )
                }
            }
        }

        if (
            showPicker
        ) {
            BluetoothDevicePicker(
                state = pickerState,
                devices = devices,
                selectedAddress = selectedAddress,
                onSelect = { device ->
                    DrivingModePreferences.setSelectedDevice(
                        context,
                        device.address,
                        device.name
                    )

                    selectedAddress =
                        device.address

                    selectedName =
                        device.name

                    autoEnabled =
                        true

                    DrivingModePreferences.setBluetoothAutoEnabled(
                        context,
                        true
                    )

                    showPicker =
                        false
                },
                onRefresh = {
                    refreshDevices()
                },
                onRequestPermission = {
                    openPickerAfterPermission =
                        true

                    permissionLauncher.launch(
                        BluetoothDevices.missingPermissions(
                            context
                        )
                    )
                },
                onDismiss = {
                    closePicker()
                }
            )
        }
    }
}

@Composable
private fun SettingsCardRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    AuralArcCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        backgroundColor = AuralArcStyle.Surface,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(
                    1f
                )
            ) {
                Text(
                    text = title,
                    color = AuralArcStyle.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = AuralArcStyle.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(
                        top = 4.dp
                    )
                )
            }

            trailing()
        }
    }
}