package com.keiranhaas.auralarc.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.keiranhaas.auralarc.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private const val FILE_NAME = "crash_log.txt"
    private const val MAX_CHARS = 60_000

    fun install(
        context: Context
    ) {
        val appContext =
            context.applicationContext

        val previousHandler =
            Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                append(
                    appContext,
                    thread,
                    error
                )
            } catch (_: Throwable) {
            }

            previousHandler?.uncaughtException(
                thread,
                error
            )
        }
    }

    fun readLog(
        context: Context
    ): String {
        return try {
            file(
                context
            ).readText()
        } catch (_: Exception) {
            ""
        }
    }

    fun deviceSummary(): String {
        return "AuralArc ${BuildConfig.VERSION_NAME}\n" +
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                "Device ${Build.MANUFACTURER} ${Build.MODEL}"
    }

    private fun append(
        context: Context,
        thread: Thread,
        error: Throwable
    ) {
        val timestamp =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
            ).format(
                Date()
            )

        val entry =
            buildString {
                append("=== ")
                append(timestamp)
                append(" ===\n")
                append(deviceSummary())
                append("\nThread: ")
                append(thread.name)
                append('\n')
                append(Log.getStackTraceString(error))
                append("\n\n")
            }

        val target =
            file(
                context
            )

        val existing =
            if (target.exists()) {
                target.readText()
            } else {
                ""
            }

        target.writeText(
            (existing + entry).takeLast(
                MAX_CHARS
            )
        )
    }

    private fun file(
        context: Context
    ): File {
        return File(
            context.filesDir,
            FILE_NAME
        )
    }
}