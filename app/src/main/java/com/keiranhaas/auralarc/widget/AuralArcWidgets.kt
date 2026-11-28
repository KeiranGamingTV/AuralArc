package com.keiranhaas.auralarc.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

class AuralArcSmallWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode =
        SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        AuralArcWidgetUpdater.start(context)

        val snapshot =
            WidgetSnapshot.load(
                context = context,
                artworkDp = 96
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
        SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        AuralArcWidgetUpdater.start(context)

        val snapshot =
            WidgetSnapshot.load(
                context = context,
                artworkDp = 240
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
        SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        AuralArcWidgetUpdater.start(context)

        val snapshot =
            WidgetSnapshot.load(
                context = context,
                artworkDp = 320
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