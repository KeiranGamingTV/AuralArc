package com.keiranhaas.auralarc

import android.app.Application
import com.keiranhaas.auralarc.utils.CrashLogger

class AuralArcApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CrashLogger.install(
            this
        )
    }
}