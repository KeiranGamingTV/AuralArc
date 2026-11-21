package com.keiranhaas.auralarc.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/*
 * Three home screen widgets that share one design language:
 * Small (2x1), Medium (4x2) and Large (4x4).
 */

class AuralArcSmallWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode =
        SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        AuralArcWidgetUpdater.start(context)

        val snapshot =
            WidgetSnapshot.load(
                context = context,
                artworkDp = 52
            )

        provideContent {
            SmallWidgetContent(
                context = context,
                snapshot = snapshot
            )
        }
    }
}

class AuralArcMediumWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode =
        SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        AuralArcWidgetUpdater.start(context)

        val snapshot =
            WidgetSnapshot.load(
                context = context,
                artworkDp = 88
            )

        provideContent {
            MediumWidgetContent(
                context = context,
                snapshot = snapshot
            )
        }
    }
}

class AuralArcLargeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode =
        SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        AuralArcWidgetUpdater.start(context)

        val snapshot =
            WidgetSnapshot.load(
                context = context,
                artworkDp = 190
            )

        provideContent {
            LargeWidgetContent(
                context = context,
                snapshot = snapshot
            )
        }
    }
}

class AuralArcSmallWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget =
        AuralArcSmallWidget()
}

class AuralArcMediumWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget =
        AuralArcMediumWidget()
}

class AuralArcLargeWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget =
        AuralArcLargeWidget()
}