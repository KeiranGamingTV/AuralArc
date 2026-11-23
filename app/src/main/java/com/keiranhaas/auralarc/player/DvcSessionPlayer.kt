package com.keiranhaas.auralarc.player

import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.util.concurrent.CopyOnWriteArraySet

@UnstableApi
class DvcSessionPlayer(
    player: Player
) : ForwardingPlayer(
    player
) {

    private val trackedListeners =
        CopyOnWriteArraySet<Player.Listener>()

    private fun remote(): Boolean =
        DirectVolumeControl.active.value

    override fun addListener(
        listener: Player.Listener
    ) {
        trackedListeners.add(
            listener
        )

        super.addListener(
            listener
        )
    }

    override fun removeListener(
        listener: Player.Listener
    ) {
        trackedListeners.remove(
            listener
        )

        super.removeListener(
            listener
        )
    }

    override fun getDeviceInfo(): DeviceInfo {
        if (
            !remote()
        ) {
            return super.getDeviceInfo()
        }

        return DeviceInfo.Builder(
            DeviceInfo.PLAYBACK_TYPE_REMOTE
        )
            .setMinVolume(
                0
            )
            .setMaxVolume(
                100
            )
            .build()
    }

    override fun getDeviceVolume(): Int {
        return if (
            remote()
        ) {
            DirectVolumeControl.level.intValue
        } else {
            super.getDeviceVolume()
        }
    }

    override fun isDeviceMuted(): Boolean {
        return if (
            remote()
        ) {
            DirectVolumeControl.level.intValue == 0
        } else {
            super.isDeviceMuted()
        }
    }

    override fun setDeviceVolume(
        volume: Int,
        flags: Int
    ) {
        if (
            remote()
        ) {
            DirectVolumeControl.setLevel(
                volume
            )
        } else {
            super.setDeviceVolume(
                volume,
                flags
            )
        }
    }

    override fun increaseDeviceVolume(
        flags: Int
    ) {
        if (
            remote()
        ) {
            DirectVolumeControl.adjust(
                DirectVolumeControl.VOLUME_STEP
            )
        } else {
            super.increaseDeviceVolume(
                flags
            )
        }
    }

    override fun decreaseDeviceVolume(
        flags: Int
    ) {
        if (
            remote()
        ) {
            DirectVolumeControl.adjust(
                -DirectVolumeControl.VOLUME_STEP
            )
        } else {
            super.decreaseDeviceVolume(
                flags
            )
        }
    }

    override fun setDeviceMuted(
        muted: Boolean,
        flags: Int
    ) {
        if (
            !remote()
        ) {
            super.setDeviceMuted(
                muted,
                flags
            )
        }
    }

    override fun getAvailableCommands(): Player.Commands {
        val base =
            super.getAvailableCommands()

        if (
            !remote()
        ) {
            return base
        }

        return base.buildUpon()
            .addAll(
                Player.COMMAND_GET_DEVICE_VOLUME,
                Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
                Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS
            )
            .build()
    }

    override fun isCommandAvailable(
        command: Int
    ): Boolean {
        return availableCommands.contains(
            command
        )
    }

    fun notifyDvcChanged() {
        val info =
            deviceInfo

        val commands =
            availableCommands

        trackedListeners.forEach { listener ->
            listener.onDeviceInfoChanged(
                info
            )

            listener.onAvailableCommandsChanged(
                commands
            )

            listener.onDeviceVolumeChanged(
                deviceVolume,
                isDeviceMuted
            )
        }
    }

    fun notifyVolumeChanged() {
        trackedListeners.forEach { listener ->
            listener.onDeviceVolumeChanged(
                deviceVolume,
                isDeviceMuted
            )
        }
    }
}