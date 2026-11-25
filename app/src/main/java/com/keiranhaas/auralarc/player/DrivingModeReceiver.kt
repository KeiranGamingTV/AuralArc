package com.keiranhaas.auralarc.player

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.keiranhaas.auralarc.MainActivity
import com.keiranhaas.auralarc.R
import com.keiranhaas.auralarc.storage.DrivingModePreferences

class DrivingModeReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) {
            return
        }

        // Bluetooth activation is optional. Driving Mode itself
        // can still be entered directly from within the app.
        if (!DrivingModePreferences.bluetoothAutoEnabled(context)) {
            return
        }

        val selectedAddress =
            DrivingModePreferences.selectedDeviceAddress(context)
                ?: return

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        @Suppress("DEPRECATION")
        val device =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
            } else {
                intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE
                )
            } ?: return

        if (device.address != selectedAddress) {
            return
        }

        // Activate Driving Mode as soon as the selected device connects.
        DrivingModePreferences.setDrivingModeEnabled(
            context,
            true
        )

        val launchIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            putExtra(
                MainActivity.EXTRA_OPEN_DRIVING_MODE,
                true
            )

            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        showActivationNotification(
            context,
            launchIntent
        )

        try {
            context.startActivity(launchIntent)
        } catch (_: Exception) {
        }
    }

    private fun showActivationNotification(
        context: Context,
        launchIntent: Intent
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Driving Mode",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Driving Mode ready")
            .setContentText("Tap to open AuralArc Driving Mode")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(
            NOTIFICATION_ID,
            notification
        )
    }

    companion object {
        private const val CHANNEL =
            "driving_mode_activation"

        private const val NOTIFICATION_ID = 4102
    }
}