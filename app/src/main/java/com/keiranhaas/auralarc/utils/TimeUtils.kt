package com.keiranhaas.auralarc.utils

fun formatTime(
    millis: Long
): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(
        minutes,
        seconds
    )
}