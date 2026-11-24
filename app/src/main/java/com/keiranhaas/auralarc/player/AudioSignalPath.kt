package com.keiranhaas.auralarc.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import com.keiranhaas.auralarc.storage.AdvancedAudioPreferences
import com.keiranhaas.auralarc.storage.CrossfadePreferences

object AudioSignalPath {

    @OptIn(UnstableApi::class)
    fun sourceRows(
        context: Context
    ): List<Pair<String, String>> {
        val format =
            try {
                PlayerManager.getPlayer(
                    context
                ).audioFormat
            } catch (_: Exception) {
                null
            }

        if (
            format == null
        ) {
            return listOf(
                "Status" to "Nothing is playing"
            )
        }

        val rows =
            mutableListOf<Pair<String, String>>()

        val mime =
            format.sampleMimeType

        if (
            mime != null
        ) {
            val codecDetail =
                format.codecs

            rows +=
                "Codec" to buildString {
                    append(
                        friendlyCodec(
                            mime
                        )
                    )

                    if (
                        !codecDetail.isNullOrBlank()
                    ) {
                        append(
                            " ($codecDetail)"
                        )
                    }
                }
        }

        if (
            format.sampleRate != Format.NO_VALUE
        ) {
            rows +=
                "Sample rate" to formatHz(
                    format.sampleRate
                )
        }

        if (
            format.channelCount != Format.NO_VALUE
        ) {
            rows +=
                "Channels" to when (
                    format.channelCount
                ) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    else -> "${format.channelCount} channels"
                }
        }

        val bitrate =
            listOf(
                format.bitrate,
                format.averageBitrate,
                format.peakBitrate
            ).firstOrNull {
                it > 0
            }

        rows +=
            "Bitrate" to if (
                bitrate != null
            ) {
                "${bitrate / 1000} kbps"
            } else {
                "Variable or unknown"
            }

        val encoding =
            when (
                format.pcmEncoding
            ) {
                C.ENCODING_PCM_8BIT -> "8-bit PCM"
                C.ENCODING_PCM_16BIT -> "16-bit PCM"
                C.ENCODING_PCM_24BIT -> "24-bit PCM"
                C.ENCODING_PCM_32BIT -> "32-bit PCM"
                C.ENCODING_PCM_FLOAT -> "32-bit float PCM"
                else -> null
            }

        if (
            encoding != null
        ) {
            rows +=
                "Encoding" to encoding
        }

        return rows
    }

    fun processingRows(
        context: Context
    ): List<Pair<String, String>> {
        val rows =
            mutableListOf<Pair<String, String>>()

        val replayGainDb =
            AudioProcessingState.replayGainDb.value

        rows +=
            "ReplayGain" to if (
                replayGainDb != 0f
            ) {
                "${"%+.1f".format(replayGainDb)} dB"
            } else {
                "None"
            }

        rows +=
            "Equalizer" to if (
                EqualizerManager.enabled.value
            ) {
                EqualizerManager.matchingPresetName()
            } else {
                "Off"
            }

        val effects =
            mutableListOf<String>()

        if (
            AudioEffectsManager.bassEnabled.value &&
            AudioEffectsManager.bassSupported.value
        ) {
            effects += "Bass boost"
        }

        if (
            AudioEffectsManager.virtualizerEnabled.value &&
            AudioEffectsManager.virtualizerSupported.value
        ) {
            effects += "Virtualizer"
        }

        if (
            AudioEffectsManager.loudnessBoostDb.value > 0f
        ) {
            effects += "Loudness +${"%.1f".format(AudioEffectsManager.loudnessBoostDb.value)} dB"
        }

        rows +=
            "Sound effects" to if (
                effects.isEmpty()
            ) {
                "Off"
            } else {
                effects.joinToString(
                    ", "
                )
            }

        rows +=
            "Direct Volume Control" to when {
                DirectVolumeControl.active.value ->
                    "Active, software volume ${DirectVolumeControl.level.intValue}%"

                AdvancedAudioPreferences.getDvcEnabled(
                    context
                ) ->
                    "Enabled, idle or bypassed"

                else ->
                    "Off"
            }

        rows +=
            "Crossfade" to if (
                CrossfadePreferences.getEnabled(
                    context
                )
            ) {
                val modeLabel =
                    if (
                        CrossfadePreferences.getMode(
                            context
                        ) == CrossfadePreferences.MODE_OVERLAP
                    ) {
                        "Overlap"
                    } else {
                        "Fade out and in"
                    }

                "$modeLabel, ${CrossfadePreferences.getSeconds(context)} s"
            } else {
                "Off"
            }

        return rows
    }

    fun outputRows(
        context: Context
    ): List<Pair<String, String>> {
        val manager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        val rows =
            mutableListOf<Pair<String, String>>()

        val device =
            routedDevice(
                manager
            )

        if (
            device != null
        ) {
            val name =
                device.productName
                    ?.toString()
                    .orEmpty()
                    .ifBlank {
                        "Unknown device"
                    }

            rows +=
                "Output device" to "$name (${deviceTypeLabel(device.type)})"

            val maxRate =
                device.sampleRates.maxOrNull()

            if (
                maxRate != null &&
                maxRate > 0
            ) {
                rows +=
                    "Device max rate" to formatHz(
                        maxRate
                    )
            }
        } else {
            rows +=
                "Output device" to "Unknown"
        }

        val mixerRate =
            manager.getProperty(
                AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE
            )?.toIntOrNull()

        if (
            mixerRate != null
        ) {
            rows +=
                "Android mixer rate" to formatHz(
                    mixerRate
                )
        }

        val frames =
            manager.getProperty(
                AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER
            )

        if (
            !frames.isNullOrBlank()
        ) {
            rows +=
                "Buffer size" to "$frames frames"
        }

        rows +=
            "Bit-perfect" to "Not reported by Android. The mixer may resample or mix before the device."

        if (
            device != null &&
            isBluetooth(
                device.type
            )
        ) {
            rows +=
                "Bluetooth codec" to "Not exposed to apps by Android"
        }

        return rows
    }

    private fun routedDevice(
        manager: AudioManager
    ): AudioDeviceInfo? {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            val attributes =
                AudioAttributes.Builder()
                    .setUsage(
                        AudioAttributes.USAGE_MEDIA
                    )
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_MUSIC
                    )
                    .build()

            val routed =
                try {
                    manager.getAudioDevicesForAttributes(
                        attributes
                    ).firstOrNull()
                } catch (_: Exception) {
                    null
                }

            if (
                routed != null
            ) {
                return routed
            }
        }

        val priority =
            buildList {
                add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)

                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    add(AudioDeviceInfo.TYPE_BLE_HEADSET)
                    add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
                }

                add(AudioDeviceInfo.TYPE_USB_HEADSET)
                add(AudioDeviceInfo.TYPE_USB_DEVICE)
                add(AudioDeviceInfo.TYPE_USB_ACCESSORY)
                add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
                add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
                add(AudioDeviceInfo.TYPE_HDMI)
                add(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                add(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
            }

        return manager.getDevices(
            AudioManager.GET_DEVICES_OUTPUTS
        ).filter {
            it.type in priority
        }.minByOrNull {
            priority.indexOf(
                it.type
            )
        }
    }

    private fun isBluetooth(
        type: Int
    ): Boolean {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                (
                                        type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                                type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                                        )
                        )
    }

    private fun deviceTypeLabel(
        type: Int
    ): String {
        return when (
            type
        ) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB headset"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB accessory"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth call audio"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            else -> {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (
                            type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                    type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                            )
                ) {
                    "Bluetooth LE"
                } else {
                    "Other"
                }
            }
        }
    }

    private fun friendlyCodec(
        mime: String
    ): String {
        return when (
            mime
        ) {
            "audio/mpeg",
            "audio/mpeg-L1",
            "audio/mpeg-L2" -> "MP3"

            "audio/mp4a-latm" -> "AAC"
            "audio/flac" -> "FLAC"
            "audio/opus" -> "Opus"
            "audio/vorbis" -> "Vorbis"
            "audio/raw" -> "PCM / WAV"
            "audio/alac" -> "ALAC"
            "audio/ac3" -> "Dolby Digital"
            "audio/eac3" -> "Dolby Digital Plus"
            else -> mime.removePrefix(
                "audio/"
            )
        }
    }

    private fun formatHz(
        hz: Int
    ): String {
        return if (
            hz % 1000 == 0
        ) {
            "${hz / 1000} kHz"
        } else {
            "${"%.1f".format(hz / 1000.0)} kHz"
        }
    }
}