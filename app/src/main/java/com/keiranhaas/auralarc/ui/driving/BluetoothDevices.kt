package com.keiranhaas.auralarc.ui.driving

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class BluetoothDeviceKind {
    CAR,
    AUDIO,
    OTHER
}

data class PairedBluetoothDevice(
    val name: String,
    val address: String,
    val kind: BluetoothDeviceKind
)

enum class BluetoothPickerState {
    READY,
    NEEDS_PERMISSION,
    BLUETOOTH_OFF,
    NOT_SUPPORTED
}

object BluetoothDevices {

    fun hasConnectPermission(
        context: Context
    ): Boolean {
        // Before Android 12 the old BLUETOOTH permission is enough.
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(
        context: Context
    ): Boolean {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** The runtime permissions the picker and activation need. */
    fun missingPermissions(
        context: Context
    ): Array<String> {
        val missing =
            mutableListOf<String>()

        if (
            !hasConnectPermission(
                context
            )
        ) {
            missing.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }

        if (
            !hasNotificationPermission(
                context
            )
        ) {
            missing.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        return missing.toTypedArray()
    }

    fun pickerState(
        context: Context
    ): BluetoothPickerState {
        val adapter =
            (
                    context.applicationContext
                        .getSystemService(
                            Context.BLUETOOTH_SERVICE
                        ) as? BluetoothManager
                    )?.adapter
                ?: return BluetoothPickerState.NOT_SUPPORTED

        if (
            !hasConnectPermission(
                context
            )
        ) {
            return BluetoothPickerState.NEEDS_PERMISSION
        }

        return try {
            if (
                adapter.isEnabled
            ) {
                BluetoothPickerState.READY
            } else {
                BluetoothPickerState.BLUETOOTH_OFF
            }
        } catch (_: SecurityException) {
            BluetoothPickerState.NEEDS_PERMISSION
        }
    }

    /** Paired devices: cars and audio gear first, then A to Z. */
    @SuppressLint("MissingPermission")
    fun loadPairedDevices(
        context: Context
    ): List<PairedBluetoothDevice> {
        if (
            pickerState(
                context
            ) != BluetoothPickerState.READY
        ) {
            return emptyList()
        }

        val adapter =
            (
                    context.applicationContext
                        .getSystemService(
                            Context.BLUETOOTH_SERVICE
                        ) as? BluetoothManager
                    )?.adapter
                ?: return emptyList()

        return try {
            adapter.bondedDevices
                .orEmpty()
                .map { device ->
                    val deviceClass =
                        device.bluetoothClass

                    val kind =
                        when {
                            deviceClass?.deviceClass ==
                                    BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                                    deviceClass?.deviceClass ==
                                    BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ->
                                BluetoothDeviceKind.CAR

                            deviceClass?.majorDeviceClass ==
                                    BluetoothClass.Device.Major.AUDIO_VIDEO ->
                                BluetoothDeviceKind.AUDIO

                            else ->
                                BluetoothDeviceKind.OTHER
                        }

                    PairedBluetoothDevice(
                        name = device.name
                            ?.takeIf { it.isNotBlank() }
                            ?: device.address,
                        address = device.address,
                        kind = kind
                    )
                }
                .sortedWith(
                    compareBy<PairedBluetoothDevice> { device ->
                        device.kind.ordinal
                    }.thenBy { device ->
                        device.name.lowercase()
                    }
                )
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}