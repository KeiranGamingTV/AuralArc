package com.keiranhaas.auralarc.navidrome

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import com.keiranhaas.auralarc.storage.LibraryCacheStore
import com.keiranhaas.auralarc.utils.ArtworkBitmapLoader
import java.util.Locale

data class DiagnosticsSection(
    val title: String,
    val rows: List<Pair<String, String>>
)

data class NavidromeServerCounts(
    val artists: Int,
    val albums: Int,
    val playlists: Int
)

data class NavidromeLibraryTotals(
    val songCount: Int,
    val totalBytes: Long
)

data class NavidromeDownloadSample(
    val bytes: Long,
    val millis: Long
)

object NavidromeDiagnostics {

    fun run(
        context: Context,
        credentials: NavidromeCredentials,
        onProgress: (String) -> Unit,
        onSection: (DiagnosticsSection) -> Unit
    ) {
        val client = NavidromeClient(credentials)
        val appContext = context.applicationContext

        onProgress("Checking the server...")
        onSection(serverSection(client))

        onProgress("Measuring ping and download speed...")
        onSection(speedSection(client))

        onProgress("Checking this phone's connection...")
        onSection(networkSection(appContext))

        onProgress("Reading the server library...")
        onSection(
            librarySection(client) { songs ->
                onProgress("Reading the server library... $songs songs so far")
            }
        )

        onProgress("Checking phone storage...")
        onSection(storageSection(appContext))
    }

    private fun failed(
        title: String,
        error: Exception
    ): DiagnosticsSection {
        return DiagnosticsSection(
            title = title,
            rows = listOf(
                "Status" to "Failed",
                "Reason" to (error.message ?: "Unknown error")
            )
        )
    }

    private fun serverSection(
        client: NavidromeClient
    ): DiagnosticsSection {
        return try {
            val response =
                client.getJson(
                    endpoint = "ping.view"
                )

            DiagnosticsSection(
                title = "Server",
                rows = listOf(
                    "Status" to "Online",
                    "Server Type" to response.optString("type", "Navidrome")
                        .ifBlank { "Navidrome" },
                    "Server Version" to response.optString("serverVersion", "")
                        .ifBlank { "Not reported" },
                    "API Version" to response.optString("version", "")
                        .ifBlank { "Not reported" },
                    "OpenSubsonic" to if (response.optBoolean("openSubsonic", false)) {
                        "Supported"
                    } else {
                        "Not reported"
                    }
                )
            )
        } catch (e: Exception) {
            failed("Server", e)
        }
    }

    private fun speedSection(
        client: NavidromeClient
    ): DiagnosticsSection {
        return try {
            val times = ArrayList<Long>()

            repeat(5) {
                val start = System.nanoTime()

                client.getJson(
                    endpoint = "ping.view"
                )

                times.add(
                    (System.nanoTime() - start) / 1_000_000L
                )
            }

            val average = times.average().toLong()

            val rating =
                when {
                    average < 60L -> "Excellent"
                    average < 150L -> "Good"
                    average < 300L -> "Fair"
                    else -> "Slow"
                }

            val rows = arrayListOf(
                "Ping (average of 5)" to "$average ms",
                "Ping (fastest / slowest)" to "${times.min()} ms / ${times.max()} ms",
                "Responsiveness" to rating
            )

            val downloadText =
                try {
                    val songId = client.getLargestSampleSongId()

                    if (songId == null) {
                        "No songs to test with"
                    } else {
                        val sample = client.measureDownload(songId)

                        if (sample.bytes < 256L * 1024L) {
                            "Test file too small to measure"
                        } else {
                            val mbps =
                                sample.bytes * 8.0 /
                                        (sample.millis / 1000.0) /
                                        1_000_000.0

                            String.format(
                                Locale.US,
                                "%.1f Mbps (%s sampled)",
                                mbps,
                                formatBytes(sample.bytes)
                            )
                        }
                    }
                } catch (e: Exception) {
                    "Failed: ${e.message ?: "Unknown error"}"
                }

            rows.add("Download Speed" to downloadText)

            DiagnosticsSection(
                title = "Speed",
                rows = rows
            )
        } catch (e: Exception) {
            failed("Speed", e)
        }
    }

    private fun networkSection(
        context: Context
    ): DiagnosticsSection {
        val title = "Phone Connection"

        return try {
            val manager =
                context.getSystemService(
                    ConnectivityManager::class.java
                ) ?: return DiagnosticsSection(
                    title,
                    listOf("Status" to "Network service unavailable")
                )

            val network =
                manager.activeNetwork ?: return DiagnosticsSection(
                    title,
                    listOf("Status" to "No active network")
                )

            val caps =
                manager.getNetworkCapabilities(network)
                    ?: return DiagnosticsSection(
                        title,
                        listOf("Status" to "No active network")
                    )

            val type =
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Other"
                }

            val signal = caps.signalStrength

            val signalText =
                if (signal == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) {
                    "Not reported by this phone"
                } else {
                    "$signal dBm (${signalQuality(type, signal)})"
                }

            val rows = arrayListOf(
                "Network Type" to type,
                "Signal Strength" to signalText
            )

            val linkMbps = caps.linkDownstreamBandwidthKbps / 1000

            if (linkMbps > 0) {
                rows.add("Reported Link Speed" to "$linkMbps Mbps (Android estimate)")
            }

            rows.add(
                "Metered Connection" to
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                            "No"
                        } else {
                            "Yes"
                        }
            )

            rows.add(
                "VPN Active" to
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            "Yes"
                        } else {
                            "No"
                        }
            )

            DiagnosticsSection(
                title = title,
                rows = rows
            )
        } catch (e: Exception) {
            failed(title, e)
        }
    }

    private fun signalQuality(
        type: String,
        dbm: Int
    ): String {
        val thresholds =
            if (type == "Cellular") {
                intArrayOf(-85, -100, -110)
            } else {
                intArrayOf(-55, -67, -75)
            }

        return when {
            dbm >= thresholds[0] -> "Excellent"
            dbm >= thresholds[1] -> "Good"
            dbm >= thresholds[2] -> "Fair"
            else -> "Weak"
        }
    }

    private fun librarySection(
        client: NavidromeClient,
        onSongsCounted: (Int) -> Unit
    ): DiagnosticsSection {
        val title = "Library on Server"

        return try {
            val counts = client.getServerCounts()

            val totals =
                client.getLibraryTotals(
                    onProgress = onSongsCounted
                )

            val average =
                if (totals.songCount > 0) {
                    totals.totalBytes / totals.songCount
                } else {
                    0L
                }

            DiagnosticsSection(
                title = title,
                rows = listOf(
                    "Songs" to totals.songCount.toString(),
                    "Albums" to counts.albums.toString(),
                    "Artists" to counts.artists.toString(),
                    "Playlists" to counts.playlists.toString(),
                    "Music Size on Server" to formatBytes(totals.totalBytes),
                    "Average Song Size" to formatBytes(average)
                )
            )
        } catch (e: Exception) {
            failed(title, e)
        }
    }

    private fun storageSection(
        context: Context
    ): DiagnosticsSection {
        val title = "Storage on This Phone"

        return try {
            val libraryCache =
                LibraryCacheStore.navidromeCacheSizeBytes(context)

            val artCache =
                ArtworkBitmapLoader.remoteArtworkCacheSizeBytes(context)

            val stat = StatFs(context.filesDir.path)

            DiagnosticsSection(
                title = title,
                rows = listOf(
                    "Library Cache" to formatBytes(libraryCache),
                    "Cover Art Cache" to formatBytes(artCache),
                    "Total Used by Navidrome" to formatBytes(libraryCache + artCache),
                    "Phone Free Space" to
                            "${formatBytes(stat.availableBytes)} of ${formatBytes(stat.totalBytes)}"
                )
            )
        } catch (e: Exception) {
            failed(title, e)
        }
    }

    private fun formatBytes(
        bytes: Long
    ): String {
        if (bytes < 1024L) {
            return "$bytes B"
        }

        val units = arrayOf("KB", "MB", "GB", "TB")

        var value = bytes.toDouble()
        var index = -1

        do {
            value /= 1024.0
            index++
        } while (value >= 1024.0 && index < units.lastIndex)

        return String.format(
            Locale.US,
            if (value >= 100.0) "%.0f %s" else "%.1f %s",
            value,
            units[index]
        )
    }
}