
package com.keiranhaas.auralarc.ui.driving

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
fun DrivingVoiceListener(
    enabled: Boolean,
    onCommand: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestOnCommand by rememberUpdatedState(onCommand)

    DisposableEffect(context, enabled) {
        // Never use an online recognizer or request microphone permission here.
        val hasMicrophonePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (
            !enabled ||
            !hasMicrophonePermission ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            onDispose { }
        } else {
            val mainHandler = Handler(Looper.getMainLooper())
            val recognizer =
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

            var active = true
            var restartPending = false

            val recognitionIntent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            lateinit var startListening: () -> Unit

            fun scheduleRestart(delayMillis: Long = 700L) {
                if (!active || restartPending) return

                restartPending = true
                mainHandler.postDelayed({
                    restartPending = false
                    if (active) {
                        startListening()
                    }
                }, delayMillis)
            }

            fun processRecognizedPhrase(phrase: String) {
                val normalized = phrase.trim().lowercase(Locale.ROOT)

                // A command must start with an explicit wake phrase.
                val wakePhrase = Regex(
                    """^\s*hey\s+(?:aural\s+arc|arc)\b[\s,.:;!?-]*(.*)$"""
                )
                val match = wakePhrase.find(normalized) ?: return
                val command = match.groupValues[1].trim()

                if (command.isNotBlank()) {
                    latestOnCommand(command)
                }
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

                override fun onPartialResults(
                    partialResults: android.os.Bundle?
                ) = Unit

                override fun onResults(results: android.os.Bundle?) {
                    val phrases = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    phrases?.firstOrNull()?.let { phrase ->
                        processRecognizedPhrase(phrase)
                    }

                    scheduleRestart()
                }

                override fun onError(error: Int) {
                    // Permission and client errors need user/app intervention;
                    // don't repeatedly restart into a permanent failure.
                    if (
                        error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                        error == SpeechRecognizer.ERROR_CLIENT
                    ) {
                        return
                    }

                    scheduleRestart(
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                            1_200L
                        } else {
                            900L
                        }
                    )
                }
            })

            startListening = {
                if (active) {
                    try {
                        recognizer.startListening(recognitionIntent)
                    } catch (_: IllegalStateException) {
                        scheduleRestart(1_200L)
                    }
                }
            }

            startListening()

            onDispose {
                active = false
                mainHandler.removeCallbacksAndMessages(null)
                recognizer.cancel()
                recognizer.destroy()
            }
        }
    }
}
