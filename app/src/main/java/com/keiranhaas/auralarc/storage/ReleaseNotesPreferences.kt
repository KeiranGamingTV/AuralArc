package com.keiranhaas.auralarc.storage

import android.content.Context

/**
 * Remembers which app version's "What's New" pop-up the user has
 * already seen, so it only appears once after each update.
 */
object ReleaseNotesPreferences {

    private const val PREFS_NAME =
        "release_notes_prefs"

    private const val KEY_LAST_SEEN_VERSION =
        "last_seen_version"

    /**
     * True when the app was updated since the pop-up was last
     * acknowledged.
     *
     * - Fresh installs record the version when first-run setup finishes
     *   (see MainActivity), so they never see a "What's New" pop-up.
     * - Installs that predate this feature have no saved version but have
     *   already completed setup, so they see the pop-up once.
     */
    fun shouldShowWhatsNew(
        context: Context,
        currentVersion: String
    ): Boolean {
        val lastSeen =
            getLastSeenVersion(
                context
            )

        if (
            lastSeen == null
        ) {
            return SetupPreferences.isSetupComplete(
                context
            )
        }

        return lastSeen != currentVersion
    }

    fun markSeen(
        context: Context,
        version: String
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LAST_SEEN_VERSION,
                version
            )
            .apply()
    }

    private fun getLastSeenVersion(
        context: Context
    ): String? {
        return context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_LAST_SEEN_VERSION,
                null
            )
    }
}