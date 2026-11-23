package com.keiranhaas.auralarc.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.Player
import com.keiranhaas.auralarc.storage.AdvancedAudioPreferences
import kotlin.math.pow
import kotlin.math.roundToInt

object DirectVolumeControl {

    private const val PREFS_NAME =
        "auralarc_dvc_state"

    private const val KEY_LEVEL =
        "level"

    private const val KEY_ENGAGED =
        "engaged"

    const val VOLUME_STEP =
        2

    val level =
        mutableIntStateOf(
            50
        )

    val active =
        mutableStateOf(
            false
        )

    private var appContext: Context? =
        null

    private var audioManager: AudioManager? =
        null

    private var sessionPlayer: DvcSessionPlayer? =
        null

    private var deviceCallbackRegistered =
        false

    private val deviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(
                addedDevices: Array<out AudioDeviceInfo>?
            ) {
                sync()
            }

            override fun onAudioDevicesRemoved(
                removedDevices: Array<out AudioDeviceInfo>?
            ) {
                sync()
            }
        }

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun attach(
        context: Context,
        player: Player
    ): Player {
        val app =
            context.applicationContext

        appContext =
            app

        val manager =
            app.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        audioManager =
            manager

        level.intValue =
            prefs(
                app
            ).getInt(
                KEY_LEVEL,
                50
            ).coerceIn(
                0,
                100
            )

        val wrapped =
            DvcSessionPlayer(
                player
            )

        sessionPlayer =
            wrapped

        if (
            !deviceCallbackRegistered
        ) {
            manager.registerAudioDeviceCallback(
                deviceCallback,
                Handler(
                    Looper.getMainLooper()
                )
            )

            deviceCallbackRegistered =
                true
        }

        sync()

        return wrapped
    }

    fun release() {
        val app =
            appContext

        val manager =
            audioManager

        if (
            app != null &&
            manager != null
        ) {
            if (
                deviceCallbackRegistered
            ) {
                manager.unregisterAudioDeviceCallback(
                    deviceCallback
                )

                deviceCallbackRegistered =
                    false
            }

            disengage(
                app,
                manager
            )
        }

        active.value =
            false

        sessionPlayer =
            null
    }

    fun sync() {
        val app =
            appContext
                ?: return

        val manager =
            audioManager
                ?: return

        val shouldBeActive =
            computeActive(
                app,
                manager
            )

        val engaged =
            prefs(
                app
            ).getBoolean(
                KEY_ENGAGED,
                false
            )

        if (
            shouldBeActive &&
            !engaged
        ) {
            engage(
                app,
                manager
            )
        } else if (
            !shouldBeActive &&
            engaged
        ) {
            disengage(
                app,
                manager
            )
        }

        active.value =
            shouldBeActive

        PlayerManager.refreshEffectivePlayerVolume()

        sessionPlayer?.notifyDvcChanged()
    }

    fun setLevel(
        value: Int
    ) {
        val app =
            appContext
                ?: return

        val safeValue =
            value.coerceIn(
                0,
                100
            )

        level.intValue =
            safeValue

        prefs(
            app
        ).edit()
            .putInt(
                KEY_LEVEL,
                safeValue
            )
            .apply()

        PlayerManager.refreshEffectivePlayerVolume()

        sessionPlayer?.notifyVolumeChanged()
    }

    fun adjust(
        delta: Int
    ) {
        setLevel(
            level.intValue + delta
        )
    }

    fun softwareGain(): Float {
        if (
            !active.value
        ) {
            return 1f
        }

        val fraction =
            level.intValue / 100f

        return fraction * fraction
    }

    fun headroomMultiplier(): Float {
        val app =
            appContext
                ?: return 1f

        if (
            active.value ||
            !EqualizerManager.enabled.value
        ) {
            return 1f
        }

        val reductionDb =
            AdvancedAudioPreferences.getDvcPreampReduction(
                app
            )

        return 10.0.pow(
            reductionDb / 20.0
        ).toFloat()
    }

    private fun engage(
        app: Context,
        manager: AudioManager
    ) {
        val maxVolume =
            manager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        val current =
            manager.getStreamVolume(
                AudioManager.STREAM_MUSIC
            )

        val startLevel =
            (
                    current * 100f /
                            maxVolume.coerceAtLeast(
                                1
                            )
                    ).roundToInt()
                .coerceIn(
                    0,
                    100
                )

        level.intValue =
            startLevel

        prefs(
            app
        ).edit()
            .putBoolean(
                KEY_ENGAGED,
                true
            )
            .putInt(
                KEY_LEVEL,
                startLevel
            )
            .apply()

        manager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            maxVolume,
            0
        )
    }

    private fun disengage(
        app: Context,
        manager: AudioManager
    ) {
        val stored =
            prefs(
                app
            )

        if (
            !stored.getBoolean(
                KEY_ENGAGED,
                false
            )
        ) {
            return
        }

        val maxVolume =
            manager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        val restored =
            (
                    level.intValue / 100f *
                            maxVolume
                    ).roundToInt()
                .coerceIn(
                    0,
                    maxVolume
                )

        manager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            restored,
            0
        )

        stored.edit()
            .putBoolean(
                KEY_ENGAGED,
                false
            )
            .apply()
    }

    private fun computeActive(
        app: Context,
        manager: AudioManager
    ): Boolean {
        if (
            !AdvancedAudioPreferences.getDvcEnabled(
                app
            )
        ) {
            return false
        }

        if (
            !PlayerManager.isPlaying.value
        ) {
            return false
        }

        if (
            AdvancedAudioPreferences.getNoDvcForBluetoothAbsoluteVolume(
                app
            ) &&
            bluetoothOutputConnected(
                manager
            )
        ) {
            return false
        }

        return true
    }

    private fun bluetoothOutputConnected(
        manager: AudioManager
    ): Boolean {
        return manager.getDevices(
            AudioManager.GET_DEVICES_OUTPUTS
        ).any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_HEARING_AID ||
                    (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    (
                                            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                                            )
                            )
        }
    }
}