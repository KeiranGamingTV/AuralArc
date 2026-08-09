package com.keiranhaas.auralarc.storage

import java.util.TreeMap

data class DuetLyricRow(
    val timeMs: Long,
    val singerOneText: String = "",
    val singerTwoText: String = "",
    val sharedText: String = ""
)

data class ParsedDuetLrc(
    val metadata: Map<String, List<String>>,
    val offsetMs: Long,
    val rows: List<DuetLyricRow>
) {
    fun metadataValues(
        key: String
    ): List<String> {
        return metadata[
            key.trim().lowercase()
        ] ?: emptyList()
    }

    fun lastMetadataValue(
        key: String
    ): String? {
        return metadataValues(
            key
        ).lastOrNull()
    }
}

object DuetLrcParser {

    private val timestampPrefixRegex =
        Regex(
            """^\s*\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]"""
        )

    private val duetLineRegex =
        Regex(
            """^\s*\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]\s*\{([12Bb])\}\s*(.*?)\s*$"""
        )

    private val metadataLineRegex =
        Regex(
            """^\s*\[([^:\]]+):(.*)\]\s*$"""
        )

    fun parse(
        rawLyrics: String
    ): ParsedDuetLrc {
        val metadataValues =
            linkedMapOf<String, MutableList<String>>()

        rawLyrics.lines().forEach { rawLine ->
            if (
                timestampPrefixRegex.containsMatchIn(
                    rawLine
                )
            ) {
                return@forEach
            }

            val metadataMatch =
                metadataLineRegex.matchEntire(
                    rawLine
                ) ?: return@forEach

            val key =
                metadataMatch.groupValues[1]
                    .trim()
                    .lowercase()

            val value =
                metadataMatch.groupValues[2]
                    .trim()

            if (
                key.isNotBlank()
            ) {
                metadataValues
                    .getOrPut(
                        key
                    ) {
                        mutableListOf()
                    }
                    .add(
                        value
                    )
            }
        }

        val offsetMs =
            metadataValues[
                "offset"
            ]
                ?.lastOrNull()
                ?.trim()
                ?.toLongOrNull()
                ?: 0L

        val rowBuilders =
            TreeMap<Long, MutableDuetLyricRow>()

        rawLyrics.lines().forEach { rawLine ->
            val lineMatch =
                duetLineRegex.matchEntire(
                    rawLine
                ) ?: return@forEach

            val baseTimeMs =
                parseTimestampMs(
                    minutesRaw = lineMatch.groupValues[1],
                    secondsRaw = lineMatch.groupValues[2],
                    fractionRaw = lineMatch.groupValues[3]
                ) ?: return@forEach

            val lyricText =
                lineMatch.groupValues[5]
                    .trim()

            if (
                lyricText.isBlank()
            ) {
                return@forEach
            }

            val adjustedTimeMs =
                (
                        baseTimeMs +
                                offsetMs
                        ).coerceAtLeast(
                        0L
                    )

            val row =
                rowBuilders.getOrPut(
                    adjustedTimeMs
                ) {
                    MutableDuetLyricRow(
                        timeMs = adjustedTimeMs
                    )
                }

            when (
                lineMatch.groupValues[4].uppercase()
            ) {
                "1" ->
                    row.singerOneText =
                        appendLine(
                            existing = row.singerOneText,
                            newLine = lyricText
                        )

                "2" ->
                    row.singerTwoText =
                        appendLine(
                            existing = row.singerTwoText,
                            newLine = lyricText
                        )

                "B" ->
                    row.sharedText =
                        appendLine(
                            existing = row.sharedText,
                            newLine = lyricText
                        )
            }
        }

        return ParsedDuetLrc(
            metadata = metadataValues.mapValues { entry ->
                entry.value.toList()
            },
            offsetMs = offsetMs,
            rows = rowBuilders.values.map { row ->
                DuetLyricRow(
                    timeMs = row.timeMs,
                    singerOneText = row.singerOneText,
                    singerTwoText = row.singerTwoText,
                    sharedText = row.sharedText
                )
            }
        )
    }

    private fun parseTimestampMs(
        minutesRaw: String,
        secondsRaw: String,
        fractionRaw: String
    ): Long? {
        val minutes =
            minutesRaw.toLongOrNull()
                ?: return null

        val seconds =
            secondsRaw.toLongOrNull()
                ?: return null

        if (
            seconds !in 0L..59L
        ) {
            return null
        }

        val fractionMs =
            when (
                fractionRaw.length
            ) {
                0 ->
                    0L

                1 ->
                    fractionRaw.toLongOrNull()
                        ?.times(
                            100L
                        )
                        ?: return null

                2 ->
                    fractionRaw.toLongOrNull()
                        ?.times(
                            10L
                        )
                        ?: return null

                3 ->
                    fractionRaw.toLongOrNull()
                        ?: return null

                else ->
                    return null
            }

        return minutes * 60_000L +
                seconds * 1_000L +
                fractionMs
    }

    private fun appendLine(
        existing: String,
        newLine: String
    ): String {
        return if (
            existing.isBlank()
        ) {
            newLine
        } else {
            "$existing\n$newLine"
        }
    }

    private data class MutableDuetLyricRow(
        val timeMs: Long,
        var singerOneText: String = "",
        var singerTwoText: String = "",
        var sharedText: String = ""
    )
}
