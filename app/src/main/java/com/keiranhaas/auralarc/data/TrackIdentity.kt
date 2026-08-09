package com.keiranhaas.auralarc.data

import android.net.Uri
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object TrackIdentity {

    fun getStableId(
        track: MusicTrack
    ): String {
        if (
            track.stableTrackId.isNotBlank()
        ) {
            return track.stableTrackId
        }

        return getStableIdFromUri(
            track.uri
        )
    }

    fun getStableIdFromUri(
        trackUri: String
    ): String {
        val navidromeSongId =
            extractNavidromeSongId(
                trackUri
            )

        if (
            navidromeSongId != null
        ) {
            return "navidrome:$navidromeSongId"
        }

        return "local:" +
                sha256(
                    trackUri
                )
    }

    private fun extractNavidromeSongId(
        trackUri: String
    ): String? {
        return try {
            val uri =
                Uri.parse(
                    trackUri
                )

            val path =
                uri.path ?: ""

            val looksLikeNavidrome =
                path.contains(
                    "stream.view"
                ) ||
                        path.contains(
                            "download.view"
                        )

            if (
                !looksLikeNavidrome
            ) {
                return null
            }

            uri.getQueryParameter(
                "id"
            )?.takeIf {
                it.isNotBlank()
            }
        } catch (
            _: Exception
        ) {
            null
        }
    }

    private fun sha256(
        value: String
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        val hash =
            digest.digest(
                value.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        return hash.joinToString(
            separator = ""
        ) {
            "%02x".format(
                it
            )
        }
    }
}