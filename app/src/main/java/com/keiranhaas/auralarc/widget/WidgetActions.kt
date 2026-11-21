package com.keiranhaas.auralarc.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import com.keiranhaas.auralarc.MainActivity
import com.keiranhaas.auralarc.player.PlaybackService

/**
 * Widget buttons reuse the same service actions as the playback
 * notification, so they also work when AuralArc is not running.
 */
object WidgetActions {

    fun playPause(context: Context): Action =
        service(context, PlaybackService.ACTION_PLAY_PAUSE)

    fun next(context: Context): Action =
        service(context, PlaybackService.ACTION_NEXT)

    fun previous(context: Context): Action =
        service(context, PlaybackService.ACTION_PREVIOUS)

    fun shuffle(context: Context): Action =
        service(context, PlaybackService.ACTION_SHUFFLE)

    fun repeat(context: Context): Action =
        service(context, PlaybackService.ACTION_REPEAT)

    fun openApp(): Action =
        actionStartActivity<MainActivity>()

    private fun service(
        context: Context,
        serviceAction: String
    ): Action {
        val intent =
            Intent(
                context.applicationContext,
                PlaybackService::class.java
            ).apply {
                action = serviceAction
            }

        return actionStartService(
            intent = intent,
            isForegroundService = true
        )
    }
}