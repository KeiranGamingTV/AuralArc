package com.keiranhaas.auralarc.navidrome

import android.content.Context

object NavidromePreferences {

    private const val PREFS_NAME =
        "navidrome_prefs"

    private const val KEY_SERVER_URL =
        "server_url"

    private const val KEY_USERNAME =
        "username"

    private const val KEY_PASSWORD =
        "password"

    fun saveCredentials(
        context: Context,
        credentials: NavidromeCredentials
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                KEY_SERVER_URL,
                credentials.serverUrl.trim().trimEnd('/')
            )
            .putString(
                KEY_USERNAME,
                credentials.username.trim()
            )
            .putString(
                KEY_PASSWORD,
                credentials.password
            )
            .apply()
    }

    fun getCredentials(
        context: Context
    ): NavidromeCredentials? {
        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val serverUrl =
            prefs.getString(
                KEY_SERVER_URL,
                null
            )

        val username =
            prefs.getString(
                KEY_USERNAME,
                null
            )

        val password =
            prefs.getString(
                KEY_PASSWORD,
                null
            )

        if (
            serverUrl.isNullOrBlank() ||
            username.isNullOrBlank() ||
            password.isNullOrBlank()
        ) {
            return null
        }

        return NavidromeCredentials(
            serverUrl = serverUrl,
            username = username,
            password = password
        )
    }

    fun clearCredentials(
        context: Context
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
    }
}