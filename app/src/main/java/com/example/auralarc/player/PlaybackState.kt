package com.example.auralarc.player

import androidx.compose.runtime.mutableStateOf

enum class AuralArcRepeatMode {
    OFF,
    ONE,
    ALL
}

object PlaybackState {

    val shuffleEnabled =
        mutableStateOf(false)

    val repeatMode =
        mutableStateOf(AuralArcRepeatMode.OFF)

    fun toggleShuffle() {
        shuffleEnabled.value =
            !shuffleEnabled.value
    }

    fun cycleRepeatMode() {
        repeatMode.value =
            when (
                repeatMode.value
            ) {
                AuralArcRepeatMode.OFF ->
                    AuralArcRepeatMode.ALL

                AuralArcRepeatMode.ALL ->
                    AuralArcRepeatMode.ONE

                AuralArcRepeatMode.ONE ->
                    AuralArcRepeatMode.OFF
            }
    }

    fun repeatText(): String {
        return when (repeatMode.value) {
            AuralArcRepeatMode.OFF ->
                "Repeat Off"

            AuralArcRepeatMode.ONE ->
                "Repeat One"

            AuralArcRepeatMode.ALL ->
                "Repeat All"
        }
    }
}