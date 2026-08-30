package com.keiranhaas.auralarc.storage

import android.content.Context

object SetupPreferences {

    private const val PREFS_NAME =
        "first_run_setup_prefs"

    private const val KEY_SETUP_COMPLETE =
        "setup_complete"

    /*
     * INTERNAL DEVELOPMENT SWITCH ONLY.
     *
     * true:
     * Show the first-run setup every time AuralArc is launched,
     * regardless of whether setup has already been completed.
     *
     * false:
     * Normal production behavior. Setup is only shown once.
     *
     * IMPORTANT:
     * Set this back to false before making a release build.
     */
    private const val FORCE_SHOW_SETUP_FOR_DEMO =
        false

    fun shouldShowSetup(
        context: Context
    ): Boolean {
        if (
            FORCE_SHOW_SETUP_FOR_DEMO
        ) {
            return true
        }

        return !isSetupComplete(
            context
        )
    }

    fun isSetupComplete(
        context: Context
    ): Boolean {
        return context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                KEY_SETUP_COMPLETE,
                false
            )
    }

    fun markSetupComplete(
        context: Context
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_SETUP_COMPLETE,
                true
            )
            .apply()
    }
}