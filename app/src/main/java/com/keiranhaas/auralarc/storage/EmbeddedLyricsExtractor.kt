package com.keiranhaas.auralarc.storage

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

enum class EmbeddedLyricsType {
    UNSYNCED,
    SYNCED,
    DUET_SYNCED
}

data class EmbeddedLyricsResult(
    val text: String,
    val type: EmbeddedLyricsType,
    val source: String
)

object EmbeddedLyricsExtractor {

    private const val MAX_READ_BYTES =
        8 * 1024 * 1024

    fun getEmbeddedLyrics(
        context: Context,
        trackUri: String
    ): EmbeddedLyricsResult? {
        val data =
            openStreamForTrack(
                context,
                trackUri
            )?.use { inputStream ->
                readMaxBytes(
                    inputStream,
                    MAX_READ_BYTES
                )
            } ?: return null

        return parseId3Lyrics(
            data
        ) ?: parseFlacLyrics(
            data
        ) ?: parseMp4Lyrics(
            data
        ) ?: parseReadableFallbackLyrics(
            data
        )
    }

    private fun openStreamForTrack(
        context: Context,
        trackUri: String
    ): InputStream? {
        return try {
            val uri =
                Uri.parse(
                    trackUri
                )

            when (
                uri.scheme?.lowercase()
            ) {
                "http",
                "https" -> {
                    val connection =
                        URL(
                            trackUri
                        ).openConnection()

                    connection.connectTimeout =
                        15000

                    connection.readTimeout =
                        30000

                    connection.getInputStream()
                }

                else -> {
                    context.contentResolver.openInputStream(
                        uri
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readMaxBytes(
        inputStream: InputStream,
        maxBytes: Int
    ): ByteArray {
        val output =
            ByteArrayOutputStream()

        val buffer =
            ByteArray(
                8192
            )

        var totalRead =
            0

        while (
            totalRead < maxBytes
        ) {
            val allowedRead =
                minOf(
                    buffer.size,
                    maxBytes - totalRead
                )

            val read =
                inputStream.read(
                    buffer,
                    0,
                    allowedRead
                )

            if (
                read <= 0
            ) {
                break
            }

            output.write(
                buffer,
                0,
                read
            )

            totalRead +=
                read
        }

        return output.toByteArray()
    }

    private fun parseId3Lyrics(
        data: ByteArray
    ): EmbeddedLyricsResult? {
        if (
            data.size < 10 ||
            data[0] != 'I'.code.toByte() ||
            data[1] != 'D'.code.toByte() ||
            data[2] != '3'.code.toByte()
        ) {
            return null
        }

        val version =
            data[3].toInt() and 0xff

        val flags =
            data[5].toInt() and 0xff

        val tagSize =
            readSynchsafeInt(
                data,
                6
            )

        if (
            tagSize <= 0 ||
            data.size < 10 + tagSize
        ) {
            return null
        }

        var tagData =
            data.copyOfRange(
                10,
                10 + tagSize
            )

        val unsynchronisation =
            flags and 0x80 != 0

        if (
            unsynchronisation
        ) {
            tagData =
                removeUnsynchronisation(
                    tagData
                )
        }

        var position =
            0

        var unsyncedResult: EmbeddedLyricsResult? =
            null

        while (
            position < tagData.size
        ) {
            val frameHeaderSize =
                if (
                    version == 2
                ) {
                    6
                } else {
                    10
                }

            if (
                position + frameHeaderSize > tagData.size
            ) {
                break
            }

            val frameId =
                if (
                    version == 2
                ) {
                    decodeAscii(
                        tagData,
                        position,
                        3
                    )
                } else {
                    decodeAscii(
                        tagData,
                        position,
                        4
                    )
                }

            if (
                frameId.isBlank() ||
                frameId.all { it == '\u0000' }
            ) {
                break
            }

            val frameSize =
                when (
                    version
                ) {
                    2 ->
                        readInt24(
                            tagData,
                            position + 3
                        )

                    4 ->
                        readSynchsafeInt(
                            tagData,
                            position + 4
                        )

                    else ->
                        readInt32(
                            tagData,
                            position + 4
                        )
                }

            if (
                frameSize <= 0 ||
                position + frameHeaderSize + frameSize > tagData.size
            ) {
                break
            }

            val frameData =
                tagData.copyOfRange(
                    position + frameHeaderSize,
                    position + frameHeaderSize + frameSize
                )

            when (
                frameId
            ) {
                "SYLT" -> {
                    val synced =
                        parseSyltFrame(
                            frameData
                        )

                    if (
                        synced != null
                    ) {
                        return synced
                    }
                }

                "USLT" -> {
                    val unsynced =
                        parseUsltFrame(
                            frameData
                        )

                    if (
                        unsynced != null
                    ) {
                        if (
                            unsynced.type == EmbeddedLyricsType.SYNCED
                        ) {
                            return unsynced
                        }

                        unsyncedResult =
                            unsynced
                    }
                }

                "TXXX" -> {
                    val textResult =
                        parseTxxxFrame(
                            frameData
                        )

                    if (
                        textResult != null
                    ) {
                        if (
                            textResult.type == EmbeddedLyricsType.SYNCED
                        ) {
                            return textResult
                        }

                        unsyncedResult =
                            textResult
                    }
                }
            }

            position +=
                frameHeaderSize + frameSize
        }

        return unsyncedResult
    }

    private fun parseUsltFrame(
        frameData: ByteArray
    ): EmbeddedLyricsResult? {
        if (
            frameData.size < 5
        ) {
            return null
        }

        val encoding =
            frameData[0].toInt() and 0xff

        var position =
            4

        val descriptionEnd =
            findEncodedTerminator(
                frameData,
                position,
                encoding
            )

        if (
            descriptionEnd < 0
        ) {
            return null
        }

        position =
            descriptionEnd + terminatorWidth(
                encoding
            )

        if (
            position >= frameData.size
        ) {
            return null
        }

        val text =
            decodeEncodedText(
                frameData.copyOfRange(
                    position,
                    frameData.size
                ),
                encoding
            ).cleanLyricsText()

        if (
            text.isBlank()
        ) {
            return null
        }

        val type =
            if (
                containsSyncedTimestamps(
                    text
                )
            ) {
                EmbeddedLyricsType.SYNCED
            } else {
                EmbeddedLyricsType.UNSYNCED
            }

        return EmbeddedLyricsResult(
            text = text,
            type = type,
            source = "ID3 USLT"
        )
    }

    private fun parseSyltFrame(
        frameData: ByteArray
    ): EmbeddedLyricsResult? {
        if (
            frameData.size < 7
        ) {
            return null
        }

        val encoding =
            frameData[0].toInt() and 0xff

        val timestampFormat =
            frameData[4].toInt() and 0xff

        var position =
            6

        val descriptionEnd =
            findEncodedTerminator(
                frameData,
                position,
                encoding
            )

        if (
            descriptionEnd < 0
        ) {
            return null
        }

        position =
            descriptionEnd + terminatorWidth(
                encoding
            )

        val lines =
            mutableListOf<String>()

        while (
            position < frameData.size - 4
        ) {
            val textEnd =
                findEncodedTerminator(
                    frameData,
                    position,
                    encoding
                )

            if (
                textEnd < 0
            ) {
                break
            }

            val lyricText =
                decodeEncodedText(
                    frameData.copyOfRange(
                        position,
                        textEnd
                    ),
                    encoding
                ).trim()

            position =
                textEnd + terminatorWidth(
                    encoding
                )

            if (
                position + 4 > frameData.size
            ) {
                break
            }

            val timestamp =
                readInt32(
                    frameData,
                    position
                )

            position +=
                4

            if (
                lyricText.isNotBlank()
            ) {
                val line =
                    if (
                        timestampFormat == 2
                    ) {
                        "${formatLrcTimestamp(timestamp.toLong())}$lyricText"
                    } else {
                        lyricText
                    }

                lines.add(
                    line
                )
            }
        }

        val text =
            lines.joinToString(
                "\n"
            ).cleanLyricsText()

        if (
            text.isBlank()
        ) {
            return null
        }

        return EmbeddedLyricsResult(
            text = text,
            type = EmbeddedLyricsType.SYNCED,
            source = "ID3 SYLT"
        )
    }

    private fun parseTxxxFrame(
        frameData: ByteArray
    ): EmbeddedLyricsResult? {
        if (
            frameData.size < 2
        ) {
            return null
        }

        val encoding =
            frameData[0].toInt() and 0xff

        val descriptionStart =
            1

        val descriptionEnd =
            findEncodedTerminator(
                frameData,
                descriptionStart,
                encoding
            )

        if (
            descriptionEnd < 0
        ) {
            return null
        }

        val description =
            decodeEncodedText(
                frameData.copyOfRange(
                    descriptionStart,
                    descriptionEnd
                ),
                encoding
            ).trim()

        val valueStart =
            descriptionEnd + terminatorWidth(
                encoding
            )

        if (
            valueStart >= frameData.size
        ) {
            return null
        }

        val value =
            decodeEncodedText(
                frameData.copyOfRange(
                    valueStart,
                    frameData.size
                ),
                encoding
            ).cleanLyricsText()

        if (
            value.isBlank()
        ) {
            return null
        }

        val descriptionUpper =
            description.uppercase()

        val looksLikeLyrics =
            descriptionUpper.contains(
                "LYRIC"
            ) ||
                    descriptionUpper.contains(
                        "SYNCED"
                    ) ||
                    descriptionUpper.contains(
                        "UNSYNC"
                    )

        if (
            !looksLikeLyrics
        ) {
            return null
        }

        val type =
            if (
                keyMeansSynced(
                    descriptionUpper
                ) ||
                containsSyncedTimestamps(
                    value
                )
            ) {
                EmbeddedLyricsType.SYNCED
            } else {
                EmbeddedLyricsType.UNSYNCED
            }

        return EmbeddedLyricsResult(
            text = value,
            type = type,
            source = "ID3 TXXX"
        )
    }

    private fun parseFlacLyrics(
        data: ByteArray
    ): EmbeddedLyricsResult? {
        if (
            data.size < 8 ||
            data[0] != 'f'.code.toByte() ||
            data[1] != 'L'.code.toByte() ||
            data[2] != 'a'.code.toByte() ||
            data[3] != 'C'.code.toByte()
        ) {
            return null
        }

        var position =
            4

        while (
            position + 4 <= data.size
        ) {
            val header =
                data[position].toInt() and 0xff

            val isLast =
                header and 0x80 != 0

            val blockType =
                header and 0x7f

            val blockLength =
                readInt24(
                    data,
                    position + 1
                )

            val blockStart =
                position + 4

            val blockEnd =
                blockStart + blockLength

            if (
                blockLength < 0 ||
                blockEnd > data.size
            ) {
                break
            }

            if (
                blockType == 4
            ) {
                val result =
                    parseVorbisCommentBlock(
                        data.copyOfRange(
                            blockStart,
                            blockEnd
                        ),
                        "FLAC Vorbis Comment"
                    )

                if (
                    result != null
                ) {
                    return result
                }
            }

            position =
                blockEnd

            if (
                isLast
            ) {
                break
            }
        }

        return null
    }

    private fun parseVorbisCommentBlock(
        block: ByteArray,
        source: String
    ): EmbeddedLyricsResult? {
        var position =
            0

        if (
            position + 4 > block.size
        ) {
            return null
        }

        val vendorLength =
            readInt32LittleEndian(
                block,
                position
            )

        position +=
            4 + vendorLength

        if (
            position + 4 > block.size
        ) {
            return null
        }

        val commentCount =
            readInt32LittleEndian(
                block,
                position
            )

        position +=
            4

        var fallbackUnsynced: EmbeddedLyricsResult? =
            null

        for (
        index in 0 until commentCount
        ) {
            if (
                position + 4 > block.size
            ) {
                break
            }

            val commentLength =
                readInt32LittleEndian(
                    block,
                    position
                )

            position +=
                4

            if (
                commentLength <= 0 ||
                position + commentLength > block.size
            ) {
                break
            }

            val comment =
                String(
                    block,
                    position,
                    commentLength,
                    Charsets.UTF_8
                )

            position +=
                commentLength

            val equalsIndex =
                comment.indexOf(
                    '='
                )

            if (
                equalsIndex <= 0
            ) {
                continue
            }

            val key =
                comment.substring(
                    0,
                    equalsIndex
                ).uppercase()

            val value =
                comment.substring(
                    equalsIndex + 1
                ).cleanLyricsText()

            if (
                value.isBlank() ||
                !key.contains(
                    "LYRIC"
                )
            ) {
                continue
            }

            val type =
                if (
                    keyMeansSynced(
                        key
                    ) ||
                    containsSyncedTimestamps(
                        value
                    )
                ) {
                    EmbeddedLyricsType.SYNCED
                } else {
                    EmbeddedLyricsType.UNSYNCED
                }

            val result =
                EmbeddedLyricsResult(
                    text = value,
                    type = type,
                    source = source
                )

            if (
                type == EmbeddedLyricsType.SYNCED
            ) {
                return result
            }

            fallbackUnsynced =
                result
        }

        return fallbackUnsynced
    }

    private fun parseMp4Lyrics(
        data: ByteArray
    ): EmbeddedLyricsResult? {
        return parseMp4Boxes(
            data = data,
            start = 0,
            end = data.size,
            depth = 0
        )
    }

    private fun parseMp4Boxes(
        data: ByteArray,
        start: Int,
        end: Int,
        depth: Int
    ): EmbeddedLyricsResult? {
        if (
            depth > 8
        ) {
            return null
        }

        var position =
            start

        while (
            position + 8 <= end &&
            position + 8 <= data.size
        ) {
            var boxSize =
                readInt32UnsignedLike(
                    data,
                    position
                )

            val typeStart =
                position + 4

            val type =
                data.copyOfRange(
                    typeStart,
                    typeStart + 4
                )

            var headerSize =
                8

            if (
                boxSize == 1L
            ) {
                if (
                    position + 16 > end
                ) {
                    break
                }

                boxSize =
                    readInt64(
                        data,
                        position + 8
                    )

                headerSize =
                    16
            } else if (
                boxSize == 0L
            ) {
                boxSize =
                    (end - position).toLong()
            }

            if (
                boxSize < headerSize ||
                position + boxSize > end
            ) {
                break
            }

            val boxEnd =
                position + boxSize.toInt()

            if (
                isMp4LyricsType(
                    type
                )
            ) {
                val result =
                    parseMp4LyricsBox(
                        data,
                        position + headerSize,
                        boxEnd
                    )

                if (
                    result != null
                ) {
                    return result
                }
            }

            val childStart =
                when (
                    decodeAsciiLenient(
                        type
                    )
                ) {
                    "moov",
                    "udta",
                    "ilst" ->
                        position + headerSize

                    "meta" ->
                        position + headerSize + 4

                    else ->
                        -1
                }

            if (
                childStart >= 0 &&
                childStart < boxEnd
            ) {
                val childResult =
                    parseMp4Boxes(
                        data,
                        childStart,
                        boxEnd,
                        depth + 1
                    )

                if (
                    childResult != null
                ) {
                    return childResult
                }
            }

            position =
                boxEnd
        }

        return null
    }

    private fun parseMp4LyricsBox(
        data: ByteArray,
        start: Int,
        end: Int
    ): EmbeddedLyricsResult? {
        var position =
            start

        while (
            position + 8 <= end
        ) {
            val size =
                readInt32UnsignedLike(
                    data,
                    position
                )

            if (
                size < 16 ||
                position + size > end
            ) {
                break
            }

            val type =
                decodeAsciiLenient(
                    data.copyOfRange(
                        position + 4,
                        position + 8
                    )
                )

            if (
                type == "data"
            ) {
                val textStart =
                    position + 16

                val textEnd =
                    position + size.toInt()

                if (
                    textStart < textEnd
                ) {
                    val text =
                        String(
                            data,
                            textStart,
                            textEnd - textStart,
                            Charsets.UTF_8
                        ).cleanLyricsText()

                    if (
                        text.isNotBlank()
                    ) {
                        val lyricsType =
                            if (
                                containsSyncedTimestamps(
                                    text
                                )
                            ) {
                                EmbeddedLyricsType.SYNCED
                            } else {
                                EmbeddedLyricsType.UNSYNCED
                            }

                        return EmbeddedLyricsResult(
                            text = text,
                            type = lyricsType,
                            source = "MP4 ©lyr"
                        )
                    }
                }
            }

            position +=
                size.toInt()
        }

        return null
    }

    private fun parseReadableFallbackLyrics(
        data: ByteArray
    ): EmbeddedLyricsResult? {
        val text =
            String(
                data,
                Charsets.UTF_8
            )

        val syncedKeys =
            listOf(
                "SYNCEDLYRICS",
                "SYNCHRONIZEDLYRICS",
                "SYNCHRONISEDLYRICS",
                "LYRICS_SYNCED"
            )

        syncedKeys.forEach { key ->
            val value =
                extractLooseTextField(
                    text,
                    key
                )

            if (
                !value.isNullOrBlank()
            ) {
                return EmbeddedLyricsResult(
                    text = value.cleanLyricsText(),
                    type = EmbeddedLyricsType.SYNCED,
                    source = "Embedded text field"
                )
            }
        }

        val unsyncedKeys =
            listOf(
                "UNSYNCEDLYRICS",
                "UNSYNCHRONIZEDLYRICS",
                "UNSYNCHRONISEDLYRICS",
                "LYRICS"
            )

        unsyncedKeys.forEach { key ->
            val value =
                extractLooseTextField(
                    text,
                    key
                )

            if (
                !value.isNullOrBlank()
            ) {
                val cleanValue =
                    value.cleanLyricsText()

                return EmbeddedLyricsResult(
                    text = cleanValue,
                    type =
                    if (
                        containsSyncedTimestamps(
                            cleanValue
                        )
                    ) {
                        EmbeddedLyricsType.SYNCED
                    } else {
                        EmbeddedLyricsType.UNSYNCED
                    },
                    source = "Embedded text field"
                )
            }
        }

        return null
    }

    private fun extractLooseTextField(
        text: String,
        key: String
    ): String? {
        val upper =
            text.uppercase()

        val marker =
            "$key="

        val index =
            upper.indexOf(
                marker
            )

        if (
            index < 0
        ) {
            return null
        }

        val valueStart =
            index + marker.length

        var valueEnd =
            valueStart

        while (
            valueEnd < text.length &&
            valueEnd - valueStart < 200000
        ) {
            val character =
                text[valueEnd]

            if (
                character == '\u0000' &&
                valueEnd > valueStart + 8
            ) {
                break
            }

            valueEnd++
        }

        return text.substring(
            valueStart,
            valueEnd
        )
    }

    private fun containsSyncedTimestamps(
        text: String
    ): Boolean {
        val lrcRegex =
            Regex(
                """\[(?:\d{1,2}:)?\d{1,2}:\d{2}(?:[.:]\d{1,3})?]"""
            )

        return lrcRegex.containsMatchIn(
            text
        )
    }

    private fun keyMeansSynced(
        key: String
    ): Boolean {
        val upper =
            key.uppercase()

        val saysUnsynced =
            upper.contains(
                "UNSYNC"
            ) ||
                    upper.contains(
                        "UNSYNCH"
                    )

        val saysSynced =
            upper.contains(
                "SYNCED"
            ) ||
                    upper.contains(
                        "SYNCHRONIZED"
                    ) ||
                    upper.contains(
                        "SYNCHRONISED"
                    )

        return saysSynced && !saysUnsynced
    }

    private fun String.cleanLyricsText(): String {
        return this
            .replace(
                "\u0000",
                ""
            )
            .replace(
                "\r\n",
                "\n"
            )
            .replace(
                "\r",
                "\n"
            )
            .trim()
    }

    private fun decodeEncodedText(
        data: ByteArray,
        encoding: Int
    ): String {
        val charset =
            charsetForEncoding(
                encoding
            )

        return try {
            String(
                data,
                charset
            )
        } catch (_: Exception) {
            String(
                data,
                Charsets.UTF_8
            )
        }
    }

    private fun charsetForEncoding(
        encoding: Int
    ): Charset {
        return when (
            encoding
        ) {
            0 ->
                Charsets.ISO_8859_1

            1 ->
                Charsets.UTF_16

            2 ->
                Charsets.UTF_16BE

            3 ->
                Charsets.UTF_8

            else ->
                Charsets.UTF_8
        }
    }

    private fun terminatorWidth(
        encoding: Int
    ): Int {
        return if (
            encoding == 1 ||
            encoding == 2
        ) {
            2
        } else {
            1
        }
    }

    private fun findEncodedTerminator(
        data: ByteArray,
        start: Int,
        encoding: Int
    ): Int {
        return if (
            encoding == 1 ||
            encoding == 2
        ) {
            var index =
                start

            while (
                index + 1 < data.size
            ) {
                if (
                    data[index] == 0.toByte() &&
                    data[index + 1] == 0.toByte()
                ) {
                    return index
                }

                index +=
                    2
            }

            -1
        } else {
            var index =
                start

            while (
                index < data.size
            ) {
                if (
                    data[index] == 0.toByte()
                ) {
                    return index
                }

                index++
            }

            -1
        }
    }

    private fun removeUnsynchronisation(
        data: ByteArray
    ): ByteArray {
        val output =
            ByteArrayOutputStream()

        var index =
            0

        while (
            index < data.size
        ) {
            val current =
                data[index]

            output.write(
                current.toInt()
            )

            if (
                current == 0xff.toByte() &&
                index + 1 < data.size &&
                data[index + 1] == 0.toByte()
            ) {
                index +=
                    2
            } else {
                index++
            }
        }

        return output.toByteArray()
    }

    private fun formatLrcTimestamp(
        milliseconds: Long
    ): String {
        val totalSeconds =
            milliseconds / 1000L

        val minutes =
            totalSeconds / 60L

        val seconds =
            totalSeconds % 60L

        val hundredths =
            (milliseconds % 1000L) / 10L

        return "[${minutes}:${seconds.toString().padStart(2, '0')}.${hundredths.toString().padStart(2, '0')}]"
    }

    private fun readSynchsafeInt(
        data: ByteArray,
        offset: Int
    ): Int {
        if (
            offset + 4 > data.size
        ) {
            return 0
        }

        return ((data[offset].toInt() and 0x7f) shl 21) or
                ((data[offset + 1].toInt() and 0x7f) shl 14) or
                ((data[offset + 2].toInt() and 0x7f) shl 7) or
                (data[offset + 3].toInt() and 0x7f)
    }

    private fun readInt24(
        data: ByteArray,
        offset: Int
    ): Int {
        if (
            offset + 3 > data.size
        ) {
            return 0
        }

        return ((data[offset].toInt() and 0xff) shl 16) or
                ((data[offset + 1].toInt() and 0xff) shl 8) or
                (data[offset + 2].toInt() and 0xff)
    }

    private fun readInt32(
        data: ByteArray,
        offset: Int
    ): Int {
        if (
            offset + 4 > data.size
        ) {
            return 0
        }

        return ((data[offset].toInt() and 0xff) shl 24) or
                ((data[offset + 1].toInt() and 0xff) shl 16) or
                ((data[offset + 2].toInt() and 0xff) shl 8) or
                (data[offset + 3].toInt() and 0xff)
    }

    private fun readInt32UnsignedLike(
        data: ByteArray,
        offset: Int
    ): Long {
        if (
            offset + 4 > data.size
        ) {
            return 0L
        }

        return ((data[offset].toLong() and 0xffL) shl 24) or
                ((data[offset + 1].toLong() and 0xffL) shl 16) or
                ((data[offset + 2].toLong() and 0xffL) shl 8) or
                (data[offset + 3].toLong() and 0xffL)
    }

    private fun readInt64(
        data: ByteArray,
        offset: Int
    ): Long {
        if (
            offset + 8 > data.size
        ) {
            return 0L
        }

        var result =
            0L

        for (
        index in 0 until 8
        ) {
            result =
                result shl 8

            result =
                result or (data[offset + index].toLong() and 0xffL)
        }

        return result
    }

    private fun readInt32LittleEndian(
        data: ByteArray,
        offset: Int
    ): Int {
        if (
            offset + 4 > data.size
        ) {
            return 0
        }

        return (data[offset].toInt() and 0xff) or
                ((data[offset + 1].toInt() and 0xff) shl 8) or
                ((data[offset + 2].toInt() and 0xff) shl 16) or
                ((data[offset + 3].toInt() and 0xff) shl 24)
    }

    private fun decodeAscii(
        data: ByteArray,
        offset: Int,
        length: Int
    ): String {
        if (
            offset + length > data.size
        ) {
            return ""
        }

        return String(
            data,
            offset,
            length,
            Charsets.ISO_8859_1
        )
    }

    private fun decodeAsciiLenient(
        data: ByteArray
    ): String {
        return String(
            data,
            Charsets.ISO_8859_1
        )
    }

    private fun isMp4LyricsType(
        type: ByteArray
    ): Boolean {
        return type.size == 4 &&
                type[0] == 0xA9.toByte() &&
                type[1] == 'l'.code.toByte() &&
                type[2] == 'y'.code.toByte() &&
                type[3] == 'r'.code.toByte()
    }
}