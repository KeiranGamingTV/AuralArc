package com.keiranhaas.auralarc.player

import kotlin.math.ln
import kotlin.math.roundToInt

class EqualizerPreset(
    val name: String,
    private val points: List<Pair<Int, Float>>
) {

    fun gainAt(
        hz: Int
    ): Float {
        val first =
            points.first()

        val last =
            points.last()

        val raw =
            when {
                hz <= first.first ->
                    first.second

                hz >= last.first ->
                    last.second

                else -> {
                    var result =
                        0f

                    for (
                    index in 0 until points.size - 1
                    ) {
                        val a =
                            points[index]

                        val b =
                            points[index + 1]

                        if (
                            hz >= a.first &&
                            hz <= b.first
                        ) {
                            val t =
                                (
                                        ln(hz.toDouble()) -
                                                ln(a.first.toDouble())
                                        ) /
                                        (
                                                ln(b.first.toDouble()) -
                                                        ln(a.first.toDouble())
                                                )

                            result =
                                (
                                        a.second +
                                                (b.second - a.second) * t
                                        ).toFloat()

                            break
                        }
                    }

                    result
                }
            }

        return (raw * 2f).roundToInt() / 2f
    }
}

object EqualizerPresets {

    val flat =
        EqualizerPreset(
            "Flat",
            listOf(
                60 to 0f,
                14000 to 0f
            )
        )

    val all =
        listOf(
            flat,
            EqualizerPreset(
                "Bass Boost",
                listOf(
                    60 to 6f,
                    250 to 3f,
                    1000 to 0f,
                    14000 to 0f
                )
            ),
            EqualizerPreset(
                "Bass Reducer",
                listOf(
                    60 to -6f,
                    250 to -3f,
                    1000 to 0f,
                    14000 to 0f
                )
            ),
            EqualizerPreset(
                "Treble Boost",
                listOf(
                    1000 to 0f,
                    4000 to 3f,
                    10000 to 6f
                )
            ),
            EqualizerPreset(
                "Vocal",
                listOf(
                    60 to -3f,
                    250 to -1f,
                    1000 to 2f,
                    3000 to 4f,
                    8000 to 0f,
                    14000 to -2f
                )
            ),
            EqualizerPreset(
                "Rock",
                listOf(
                    60 to 5f,
                    250 to 3f,
                    1000 to -1f,
                    4000 to 3f,
                    12000 to 5f
                )
            ),
            EqualizerPreset(
                "Pop",
                listOf(
                    60 to -1f,
                    250 to 2f,
                    1000 to 4f,
                    4000 to 2f,
                    12000 to -1f
                )
            ),
            EqualizerPreset(
                "Jazz",
                listOf(
                    60 to 3f,
                    250 to 2f,
                    1000 to -2f,
                    4000 to 2f,
                    12000 to 4f
                )
            ),
            EqualizerPreset(
                "Electronic",
                listOf(
                    60 to 6f,
                    250 to 3f,
                    1000 to 0f,
                    4000 to 2f,
                    12000 to 5f
                )
            )
        )
}