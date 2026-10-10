package com.keiranhaas.auralarc.storage

import java.util.TreeMap

data class DuetLyricRow(
    val timeMs: Long,
    val singerOneText: String = "",
    val singerOneColorHex: String? = null,
    val singerTwoText: String = "",
    val singerTwoColorHex: String? = null,
    val sharedText: String = "",
    val sharedColorHex: String? = null
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

    /*
     * Standard LRC timestamp prefix.
     *
     * This is intentionally separate from the full Duet line regex
     * so metadata lines and color declarations are not mistaken for
     * lyric lines.
     */
    private val timestampPrefixRegex =
        Regex(
            """^\s*\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]"""
        )

    /*
     * Duet line:
     *
     * [mm:ss.xx]{1} lyric
     * [mm:ss.xx]{2} lyric
     * [mm:ss.xx]{B} lyric
     *
     * The optional color tag is parsed separately so that aliases
     * and direct HEX values can both be resolved consistently.
     */
    private val duetLineRegex =
        Regex(
            """^\s*\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]\s*\{([12Bb])\}\s*(.*?)\s*$"""
        )

    private val metadataLineRegex =
        Regex(
            """^\s*\[([^:\]]+):(.*)\]\s*$"""
        )

    /*
     * Color declarations:
     *
     * - [#000DFF: blue]
     * - [#FF0000: red]
     * - [#329760: eurylochus]
     *
     * The name on the right can be ANY non-empty text.
     */
    private val colorMetadataRegex =
        Regex(
            """^\s*-\s*\[#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8}):\s*(.+?)\]\s*$"""
        )

    /*
     * Optional line-level color:
     *
     * <blue>
     * <Singer Name>
     * <#FF8000>
     */
    private val lineColorRegex =
        Regex(
            """^\s*<([^>]+)>\s*(.*)$"""
        )

    fun parse(
        rawLyrics: String
    ): ParsedDuetLrc {

        val metadataValues =
            linkedMapOf<String, MutableList<String>>()

        /*
         * Maps an arbitrary color/artist name to its HEX value.
         *
         * Example:
         *
         * "odysseus1" -> "#000DA1"
         * "crew"      -> "#FFFFFF"
         */
        val colorAliases =
            linkedMapOf<String, String>()

        /*
         * First pass:
         *
         * Read normal metadata and color declarations.
         */
        rawLyrics.lines().forEach { rawLine ->

            /*
             * Color declarations begin with "- [" and therefore
             * must be checked before normal metadata parsing.
             */
            val colorMatch =
                colorMetadataRegex.matchEntire(
                    rawLine
                )

            if (
                colorMatch != null
            ) {
                val hex =
                    normalizeHexColor(
                        colorMatch.groupValues[1]
                    )

                val alias =
                    colorMatch.groupValues[2]
                        .trim()

                if (
                    hex != null &&
                    alias.isNotBlank()
                ) {
                    colorAliases[
                        alias.lowercase()
                    ] =
                        hex
                }

                return@forEach
            }

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

        /*
         * Second pass:
         *
         * Parse the actual synchronized lyric lines.
         */
        rawLyrics.lines().forEach { rawLine ->

            val lineMatch =
                duetLineRegex.matchEntire(
                    rawLine
                ) ?: return@forEach

            val baseTimeMs =
                parseTimestampMs(
                    minutesRaw =
                        lineMatch.groupValues[1],
                    secondsRaw =
                        lineMatch.groupValues[2],
                    fractionRaw =
                        lineMatch.groupValues[3]
                ) ?: return@forEach

            val singer =
                lineMatch.groupValues[4]
                    .uppercase()

            /*
             * Resolve:
             *
             * <alias>
             * <#RRGGBB>
             * <#AARRGGBB>
             *
             * If there is no color tag, the result is null and
             * the UI will use its normal default column color.
             */
            val (
                colorHex,
                lyricText
            ) =
                parseLineColor(
                    lineMatch.groupValues[5],
                    colorAliases
                )

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
                        timeMs =
                            adjustedTimeMs
                    )
                }

            when (
                singer
            ) {
                "1" -> {
                    row.singerOneText =
                        appendLine(
                            existing =
                                row.singerOneText,
                            newLine =
                                lyricText
                        )

                    /*
                     * Preserve the first explicit color for
                     * this singer at this timestamp.
                     */
                    if (
                        row.singerOneColorHex == null &&
                        colorHex != null
                    ) {
                        row.singerOneColorHex =
                            colorHex
                    }
                }

                "2" -> {
                    row.singerTwoText =
                        appendLine(
                            existing =
                                row.singerTwoText,
                            newLine =
                                lyricText
                        )

                    if (
                        row.singerTwoColorHex == null &&
                        colorHex != null
                    ) {
                        row.singerTwoColorHex =
                            colorHex
                    }
                }

                "B" -> {
                    row.sharedText =
                        appendLine(
                            existing =
                                row.sharedText,
                            newLine =
                                lyricText
                        )

                    if (
                        row.sharedColorHex == null &&
                        colorHex != null
                    ) {
                        row.sharedColorHex =
                            colorHex
                    }
                }
            }
        }

        return ParsedDuetLrc(
            metadata =
                metadataValues.mapValues { entry ->
                    entry.value.toList()
                },
            offsetMs =
                offsetMs,
            rows =
                rowBuilders.values.map { row ->
                    DuetLyricRow(
                        timeMs =
                            row.timeMs,
                        singerOneText =
                            row.singerOneText,
                        singerOneColorHex =
                            row.singerOneColorHex,
                        singerTwoText =
                            row.singerTwoText,
                        singerTwoColorHex =
                            row.singerTwoColorHex,
                        sharedText =
                            row.sharedText,
                        sharedColorHex =
                            row.sharedColorHex
                    )
                }
        )
    }

    private fun parseLineColor(
        rawText: String,
        colorAliases: Map<String, String>
    ): Pair<String?, String> {

        val colorMatch =
            lineColorRegex.matchEntire(
                rawText
            )

        if (
            colorMatch == null
        ) {
            return null to rawText.trim()
        }

        val colorToken =
            colorMatch.groupValues[1]
                .trim()

        val lyricText =
            colorMatch.groupValues[2]
                .trim()

        /*
         * Direct HEX color.
         */
        val directHex =
            normalizeHexColor(
                colorToken
                    .removePrefix("#")
            )

        if (
            directHex != null
        ) {
            return directHex to lyricText
        }

        /*
         * Named/aliased color.
         *
         * Names are case-insensitive.
         */
        val aliasColor =
            colorAliases[
                colorToken.lowercase()
            ]

        return aliasColor to lyricText
    }

    private fun normalizeHexColor(
        rawHex: String
    ): String? {

        val normalized =
            rawHex
                .trim()
                .removePrefix("#")

        if (
            normalized.length != 6 &&
            normalized.length != 8
        ) {
            return null
        }

        if (
            !normalized.matches(
                Regex(
                    """[0-9A-Fa-f]+"""
                )
            )
        ) {
            return null
        }

        return "#$normalized".uppercase()
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
                    fractionRaw
                        .toLongOrNull()
                        ?.times(
                            100L
                        )
                        ?: return null

                2 ->
                    fractionRaw
                        .toLongOrNull()
                        ?.times(
                            10L
                        )
                        ?: return null

                3 ->
                    fractionRaw
                        .toLongOrNull()
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
        var singerOneColorHex: String? = null,
        var singerTwoText: String = "",
        var singerTwoColorHex: String? = null,
        var sharedText: String = "",
        var sharedColorHex: String? = null
    )
}