package com.keiranhaas.auralarc.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateResult {

    data class Available(
        val version: String,
        val url: String
    ) : UpdateResult()

    object UpToDate : UpdateResult()

    object NoReleases : UpdateResult()

    object Failed : UpdateResult()
}

object UpdateChecker {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/KeiranGamingTV/AuralArc/releases/latest"

    private const val RELEASE_PAGE_PREFIX =
        "https://github.com/"

    suspend fun check(
        currentVersion: String
    ): UpdateResult =
        withContext(
            Dispatchers.IO
        ) {
            var connection: HttpURLConnection? =
                null

            try {
                connection =
                    (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        readTimeout = 10_000

                        setRequestProperty(
                            "Accept",
                            "application/vnd.github+json"
                        )

                        setRequestProperty(
                            "User-Agent",
                            "AuralArc"
                        )
                    }

                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val json =
                            JSONObject(
                                connection.inputStream
                                    .bufferedReader()
                                    .use { reader ->
                                        reader.readText()
                                    }
                            )

                        val tag =
                            json.optString("tag_name")

                        val url =
                            json.optString("html_url")

                        if (
                            tag.isBlank() ||
                            !url.startsWith(RELEASE_PAGE_PREFIX)
                        ) {
                            UpdateResult.Failed
                        } else if (
                            isNewer(
                                tag,
                                currentVersion
                            )
                        ) {
                            UpdateResult.Available(
                                version = tag.removePrefix("v"),
                                url = url
                            )
                        } else {
                            UpdateResult.UpToDate
                        }
                    }

                    HttpURLConnection.HTTP_NOT_FOUND ->
                        UpdateResult.NoReleases

                    else ->
                        UpdateResult.Failed
                }
            } catch (_: Exception) {
                UpdateResult.Failed
            } finally {
                connection?.disconnect()
            }
        }

    private fun isNewer(
        tag: String,
        current: String
    ): Boolean {
        val latest =
            versionParts(tag)

        val installed =
            versionParts(current)

        for (
        index in 0 until maxOf(
            latest.size,
            installed.size
        )
        ) {
            val latestPart =
                latest.getOrElse(index) { 0 }

            val installedPart =
                installed.getOrElse(index) { 0 }

            if (latestPart != installedPart) {
                return latestPart > installedPart
            }
        }

        return false
    }

    private fun versionParts(
        version: String
    ): List<Int> {
        return version
            .removePrefix("v")
            .split('.')
            .map { segment ->
                segment
                    .takeWhile { character ->
                        character.isDigit()
                    }
                    .toIntOrNull() ?: 0
            }
    }
}