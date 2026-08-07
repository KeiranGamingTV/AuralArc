package com.example.auralarc.storage

import android.content.Context
import org.json.JSONArray

object EqualizerPreferences {

    private const val PREFS_NAME =
        "auralarc_equalizer_preferences"

    private const val KEY_ENABLED =
        "enabled"

    private const val KEY_BAND_GAINS =
        "band_gains"

    fun getEnabled(
        context: Context
    ): Boolean {
        return context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                KEY_ENABLED,
                false
            )
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }

    fun getBandGains(
        context: Context
    ): List<Float> {
        val raw =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_BAND_GAINS,
                    "[]"
                ) ?: "[]"

        return try {
            val array =
                JSONArray(
                    raw
                )

            val values =
                mutableListOf<Float>()

            for (
            index in 0 until array.length()
            ) {
                values.add(
                    array.optDouble(
                        index,
                        0.0
                    ).toFloat()
                        .coerceIn(
                            -12f,
                            12f
                        )
                )
            }

            values
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setBandGains(
        context: Context,
        gains: List<Float>
    ) {
        val array =
            JSONArray()

        gains.take(
            32
        ).forEach { gain ->
            array.put(
                gain.coerceIn(
                    -12f,
                    12f
                )
            )
        }

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_BAND_GAINS,
                array.toString()
            )
            .apply()
    }
}