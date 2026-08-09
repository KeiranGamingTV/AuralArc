package com.keiranhaas.auralarc.utils

fun formatDuration(
    duration: Long
): String {

    val seconds = duration / 1000

    val minutes = seconds / 60

    val remainingSeconds = seconds % 60

    return "%d:%02d".format(
        minutes,
        remainingSeconds
    )
}