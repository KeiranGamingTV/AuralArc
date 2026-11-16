package com.keiranhaas.auralarc.storage

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DrivingModePreferences {

    private const val PREFS = "auralarc_driving_mode"

    private const val KEY_MODE_ENABLED =
        "driving_mode_enabled"

    private const val KEY_AUTO =
        "bluetooth_auto_enabled"

    private const val KEY_DEVICE_ADDRESS =
        "bluetooth_device_address"

    private const val KEY_DEVICE_NAME =
        "bluetooth_device_name"

    private var initialized = false

    var drivingModeEnabled by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        if (initialized) return

        drivingModeEnabled = isDrivingModeEnabled(context)
        initialized = true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    fun isDrivingModeEnabled(
        context: Context
    ): Boolean {
        return prefs(context).getBoolean(
            KEY_MODE_ENABLED,
            false
        )
    }

    fun setDrivingModeEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context).edit()
            .putBoolean(KEY_MODE_ENABLED, enabled)
            .apply()

        drivingModeEnabled = enabled
        initialized = true
    }

    fun bluetoothAutoEnabled(
        context: Context
    ): Boolean {
        return prefs(context).getBoolean(
            KEY_AUTO,
            false
        )
    }

    fun setBluetoothAutoEnabled(
        context: Context,
        enabled: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(KEY_AUTO, enabled)
            .apply()
    }

    fun selectedDeviceAddress(
        context: Context
    ): String? {
        return prefs(context).getString(
            KEY_DEVICE_ADDRESS,
            null
        )
    }

    fun selectedDeviceName(
        context: Context
    ): String? {
        return prefs(context).getString(
            KEY_DEVICE_NAME,
            null
        )
    }

    fun setSelectedDevice(
        context: Context,
        address: String?,
        name: String?
    ) {
        prefs(context)
            .edit()
            .putString(KEY_DEVICE_ADDRESS, address)
            .putString(KEY_DEVICE_NAME, name)
            .apply()
    }
}