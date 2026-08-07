package com.example.auralarc.utils

import com.example.auralarc.data.MusicTrack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun audioQualitySummary(
    track: MusicTrack?
): String {
    if (
        track == null
    ) {
        return "Audio quality unavailable"
    }

    val codec =
        track.audioCodec.ifBlank {
            codecLabelFromMimeOrName(
                mimeType = track.mimeType,
                nameOrUri = track.uri
            )
        }

    val bitDepth =
        if (
            track.bitDepth > 0
        ) {
            "${track.bitDepth}bit"
        } else {
            ""
        }

    val sampleRate =
        formatSampleRate(
            track.sampleRate
        )

    val parts =
        listOf(
            codec,
            bitDepth,
            sampleRate
        ).filter {
            it.isNotBlank()
        }

    return if (
        parts.isEmpty()
    ) {
        "Audio quality unavailable"
    } else {
        parts.joinToString(
            " "
        )
    }
}

fun audioDetailRows(
    track: MusicTrack
): List<Pair<String, String>> {
    return listOf(
        "Now Playing Label" to audioQualitySummary(
            track
        ),
        "Codec" to valueOrUnavailable(
            track.audioCodec.ifBlank {
                codecLabelFromMimeOrName(
                    mimeType = track.mimeType,
                    nameOrUri = track.uri
                )
            }
        ),
        "MIME Type" to valueOrUnavailable(
            track.mimeType
        ),
        "Container" to valueOrUnavailable(
            track.container
        ),
        "File Extension" to valueOrUnavailable(
            track.fileExtension
        ),
        "Bit Depth" to if (
            track.bitDepth > 0
        ) {
            "${track.bitDepth}-bit"
        } else {
            "Unavailable"
        },
        "Sample Rate" to valueOrUnavailable(
            formatSampleRate(
                track.sampleRate
            )
        ),
        "Bitrate" to formatBitrate(
            track.bitrate
        ),
        "Channels" to formatChannels(
            track.channelCount
        ),
        "Lossless / Lossy" to formatLossless(
            track.lossless
        ),
        "HD Badge Eligible" to if (
            track.isHighResolution
        ) {
            "Yes — 24bit or higher"
        } else {
            "No"
        }
    )
}

fun technicalAudioRows(
    track: MusicTrack
): List<Pair<String, String>> {
    return listOf(
        "Extractor Track Count" to intOrUnavailable(
            track.extractorTrackCount
        ),
        "Audio Track Count" to intOrUnavailable(
            track.audioTrackCount
        ),
        "Audio Track Index" to if (
            track.audioTrackIndex >= 0
        ) {
            track.audioTrackIndex.toString()
        } else {
            "Unavailable"
        },
        "PCM Encoding" to formatPcmEncoding(
            track.pcmEncoding
        ),
        "Max Input Size" to if (
            track.maxInputSize > 0
        ) {
            "${track.maxInputSize} bytes"
        } else {
            "Unavailable"
        },
        "Encoder Delay" to intOrUnavailable(
            track.encoderDelay
        ),
        "Encoder Padding" to intOrUnavailable(
            track.encoderPadding
        ),
        "Language" to valueOrUnavailable(
            track.language
        ),
        "Codec Profile" to intOrUnavailable(
            track.profile
        ),
        "Codec Level" to intOrUnavailable(
            track.level
        )
    )
}

fun sourceAndFileRows(
    track: MusicTrack
): List<Pair<String, String>> {
    return listOf(
        "Source Type" to valueOrUnavailable(
            track.sourceType
        ),
        "Source Path" to valueOrUnavailable(
            track.sourcePath
        ),
        "URI" to valueOrUnavailable(
            track.uri
        ),
        "File Size" to formatFileSize(
            track.fileSizeBytes
        ),
        "Date Added" to formatDateMillis(
            track.dateAddedMillis
        ),
        "Date Modified" to formatDateMillis(
            track.dateModifiedMillis
        ),
        "Album Art Path" to valueOrUnavailable(
            track.albumArtPath.orEmpty()
        )
    )
}

fun musicMetadataRows(
    track: MusicTrack
): List<Pair<String, String>> {
    return listOf(
        "Title" to valueOrUnavailable(
            track.title
        ),
        "Artist" to valueOrUnavailable(
            track.artist
        ),
        "Album" to valueOrUnavailable(
            track.album
        ),
        "Track Number" to intOrUnavailable(
            track.trackNumber
        ),
        "Disc Number" to intOrUnavailable(
            track.discNumber
        ),
        "Duration" to formatDetailedDuration(
            track.duration
        ),
        "Release Date" to valueOrUnavailable(
            track.releaseDate
        ),
        "Release Year" to intOrUnavailable(
            track.releaseYear
        ),
        "Track ID" to track.id.toString(),
        "Album ID" to track.albumId.toString()
    )
}

fun replayGainRows(
    track: MusicTrack
): List<Pair<String, String>> {
    return listOf(
        "Track Gain" to formatReplayGain(
            track.replayGainTrackGain
        ),
        "Album Gain" to formatReplayGain(
            track.replayGainAlbumGain
        ),
        "Track Peak" to formatReplayPeak(
            track.replayGainTrackPeak
        ),
        "Album Peak" to formatReplayPeak(
            track.replayGainAlbumPeak
        )
    )
}

fun codecLabelFromMimeOrName(
    mimeType: String,
    nameOrUri: String
): String {
    val mime =
        mimeType.lowercase(
            Locale.US
        )

    val name =
        nameOrUri.lowercase(
            Locale.US
        )

    return when {
        "flac" in mime || name.endsWith(".flac") ->
            "FLAC"

        "mpeg" in mime || name.endsWith(".mp3") ->
            "MP3"

        "mp4a" in mime ||
                "aac" in mime ||
                name.endsWith(".aac") ||
                name.endsWith(".m4a") ->
            "AAC"

        "wav" in mime ||
                "wave" in mime ||
                name.endsWith(".wav") ->
            "WAV"

        "opus" in mime || name.endsWith(".opus") ->
            "OPUS"

        "ogg" in mime || name.endsWith(".ogg") ->
            "OGG"

        "alac" in mime || name.endsWith(".alac") ->
            "ALAC"

        "aiff" in mime ||
                name.endsWith(".aiff") ||
                name.endsWith(".aif") ->
            "AIFF"

        "amr" in mime || name.endsWith(".amr") ->
            "AMR"

        mime.isNotBlank() ->
            mime.substringAfterLast(
                "/"
            ).uppercase(
                Locale.US
            )

        else ->
            ""
    }
}

fun containerFromNameOrMime(
    nameOrUri: String,
    mimeType: String
): String {
    val extension =
        extensionFromNameOrUri(
            nameOrUri
        )

    return when {
        extension.isNotBlank() ->
            extension.uppercase(
                Locale.US
            )

        mimeType.isNotBlank() ->
            mimeType.substringAfterLast(
                "/"
            ).uppercase(
                Locale.US
            )

        else ->
            ""
    }
}

fun extensionFromNameOrUri(
    nameOrUri: String
): String {
    val clean =
        nameOrUri.substringBefore(
            "?"
        ).substringBefore(
            "#"
        )

    return clean.substringAfterLast(
        '.',
        ""
    ).lowercase(
        Locale.US
    )
}

fun inferLossless(
    mimeType: String,
    nameOrUri: String
): Boolean? {
    val mime =
        mimeType.lowercase(
            Locale.US
        )

    val name =
        nameOrUri.lowercase(
            Locale.US
        )

    return when {
        "flac" in mime ||
                "alac" in mime ||
                "wav" in mime ||
                "wave" in mime ||
                "aiff" in mime ||
                name.endsWith(".flac") ||
                name.endsWith(".alac") ||
                name.endsWith(".wav") ||
                name.endsWith(".aiff") ||
                name.endsWith(".aif") ->
            true

        "mpeg" in mime ||
                "mp4a" in mime ||
                "aac" in mime ||
                "opus" in mime ||
                "ogg" in mime ||
                name.endsWith(".mp3") ||
                name.endsWith(".aac") ||
                name.endsWith(".m4a") ||
                name.endsWith(".opus") ||
                name.endsWith(".ogg") ->
            false

        else ->
            null
    }
}

fun formatSampleRate(
    sampleRate: Int
): String {
    if (
        sampleRate <= 0
    ) {
        return ""
    }

    val khz =
        sampleRate / 1000.0

    return if (
        sampleRate % 1000 == 0
    ) {
        "${sampleRate / 1000}kHz"
    } else {
        String.format(
            Locale.US,
            "%.1fkHz",
            khz
        )
    }
}

fun formatBitrate(
    bitrate: Int
): String {
    if (
        bitrate <= 0
    ) {
        return "Unavailable"
    }

    return "${bitrate / 1000} kbps"
}

fun formatChannels(
    channelCount: Int
): String {
    return when (
        channelCount
    ) {
        1 ->
            "1 channel / Mono"

        2 ->
            "2 channels / Stereo"

        6 ->
            "6 channels / 5.1"

        8 ->
            "8 channels / 7.1"

        in 3..Int.MAX_VALUE ->
            "$channelCount channels"

        else ->
            "Unavailable"
    }
}

fun formatFileSize(
    bytes: Long
): String {
    if (
        bytes <= 0L
    ) {
        return "Unavailable"
    }

    val kb =
        bytes / 1024.0

    val mb =
        kb / 1024.0

    val gb =
        mb / 1024.0

    return when {
        gb >= 1.0 ->
            String.format(
                Locale.US,
                "%.2f GB",
                gb
            )

        mb >= 1.0 ->
            String.format(
                Locale.US,
                "%.2f MB",
                mb
            )

        kb >= 1.0 ->
            String.format(
                Locale.US,
                "%.2f KB",
                kb
            )

        else ->
            "$bytes bytes"
    }
}

fun formatDetailedDuration(
    durationMillis: Long
): String {
    if (
        durationMillis <= 0L
    ) {
        return "Unavailable"
    }

    val totalSeconds =
        durationMillis / 1000L

    val hours =
        totalSeconds / 3600L

    val minutes =
        (totalSeconds % 3600L) / 60L

    val seconds =
        totalSeconds % 60L

    return if (
        hours > 0L
    ) {
        String.format(
            Locale.US,
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    } else {
        String.format(
            Locale.US,
            "%d:%02d",
            minutes,
            seconds
        )
    }
}

fun formatDateMillis(
    millis: Long
): String {
    if (
        millis <= 0L
    ) {
        return "Unavailable"
    }

    return try {
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        ).format(
            Date(
                millis
            )
        )
    } catch (_: Exception) {
        "Unavailable"
    }
}

fun formatLossless(
    lossless: Boolean?
): String {
    return when (
        lossless
    ) {
        true ->
            "Lossless"

        false ->
            "Lossy"

        null ->
            "Unknown"
    }
}

fun formatPcmEncoding(
    pcmEncoding: Int
): String {
    return when (
        pcmEncoding
    ) {
        2 ->
            "PCM 16-bit"

        3 ->
            "PCM 8-bit"

        4 ->
            "PCM float"

        0 ->
            "Unavailable"

        else ->
            "PCM encoding $pcmEncoding"
    }
}

fun formatReplayGain(
    value: Float?
): String {
    return if (
        value == null
    ) {
        "Unavailable"
    } else {
        String.format(
            Locale.US,
            "%.2f dB",
            value
        )
    }
}

fun formatReplayPeak(
    value: Float?
): String {
    return if (
        value == null
    ) {
        "Unavailable"
    } else {
        String.format(
            Locale.US,
            "%.6f",
            value
        )
    }
}

fun valueOrUnavailable(
    value: String
): String {
    return value.ifBlank {
        "Unavailable"
    }
}

fun intOrUnavailable(
    value: Int
): String {
    return if (
        value > 0
    ) {
        value.toString()
    } else {
        "Unavailable"
    }
}