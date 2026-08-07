package com.example.auralarc.utils

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build

data class AudioFormatMetadata(
    val audioCodec: String = "",
    val mimeType: String = "",
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val channelCount: Int = 0,
    val genre: String = "",

    val fileSizeBytes: Long = 0L,
    val dateModifiedMillis: Long = 0L,

    val container: String = "",
    val fileExtension: String = "",
    val sourceType: String = "",
    val sourcePath: String = "",
    val lossless: Boolean? = null,

    val audioTrackCount: Int = 0,
    val extractorTrackCount: Int = 0,
    val audioTrackIndex: Int = -1,

    val pcmEncoding: Int = 0,
    val maxInputSize: Int = 0,
    val encoderDelay: Int = 0,
    val encoderPadding: Int = 0,

    val language: String = "",
    val profile: Int = 0,
    val level: Int = 0,

    val metadataWarning: String = ""
)

object AudioMetadataReader {

    private const val KEY_BITS_PER_SAMPLE =
        "bits-per-sample"

    private const val KEY_ENCODER_DELAY =
        "encoder-delay"

    private const val KEY_ENCODER_PADDING =
        "encoder-padding"

    fun read(
        context: Context,
        uri: Uri,
        fallbackName: String = "",
        fileSizeBytes: Long = 0L,
        dateModifiedMillis: Long = 0L,
        sourceType: String = "",
        sourcePath: String = ""
    ): AudioFormatMetadata {
        val extractorMetadata =
            readWithExtractor(
                context = context,
                uri = uri,
                fallbackName = fallbackName,
                fileSizeBytes = fileSizeBytes,
                dateModifiedMillis = dateModifiedMillis,
                sourceType = sourceType,
                sourcePath = sourcePath
            )

        val retrieverMetadata =
            readWithRetriever(
                context = context,
                uri = uri,
                fallbackName = fallbackName,
                fileSizeBytes = fileSizeBytes,
                dateModifiedMillis = dateModifiedMillis,
                sourceType = sourceType,
                sourcePath = sourcePath
            )

        val finalMime =
            extractorMetadata.mimeType.ifBlank {
                retrieverMetadata.mimeType
            }

        val finalName =
            fallbackName.ifBlank {
                sourcePath.ifBlank {
                    uri.toString()
                }
            }

        return AudioFormatMetadata(
            audioCodec =
            extractorMetadata.audioCodec.ifBlank {
                retrieverMetadata.audioCodec.ifBlank {
                    codecLabelFromMimeOrName(
                        mimeType = finalMime,
                        nameOrUri = finalName
                    )
                }
            },
            mimeType = finalMime,
            bitrate =
            if (
                extractorMetadata.bitrate > 0
            ) {
                extractorMetadata.bitrate
            } else {
                retrieverMetadata.bitrate
            },
            sampleRate =
            if (
                extractorMetadata.sampleRate > 0
            ) {
                extractorMetadata.sampleRate
            } else {
                retrieverMetadata.sampleRate
            },
            bitDepth =
            if (
                retrieverMetadata.bitDepth > 0
            ) {
                retrieverMetadata.bitDepth
            } else {
                extractorMetadata.bitDepth
            },
            channelCount =
            if (
                extractorMetadata.channelCount > 0
            ) {
                extractorMetadata.channelCount
            } else {
                retrieverMetadata.channelCount
            },
            genre = retrieverMetadata.genre,
            fileSizeBytes =
            fileSizeBytes.coerceAtLeast(
                extractorMetadata.fileSizeBytes
            ),
            dateModifiedMillis =
            dateModifiedMillis.coerceAtLeast(
                extractorMetadata.dateModifiedMillis
            ),
            container =
            extractorMetadata.container.ifBlank {
                retrieverMetadata.container.ifBlank {
                    containerFromNameOrMime(
                        nameOrUri = finalName,
                        mimeType = finalMime
                    )
                }
            },
            fileExtension =
            extractorMetadata.fileExtension.ifBlank {
                retrieverMetadata.fileExtension.ifBlank {
                    extensionFromNameOrUri(
                        finalName
                    )
                }
            },
            sourceType = sourceType,
            sourcePath = sourcePath,
            lossless =
            extractorMetadata.lossless
                ?: retrieverMetadata.lossless
                ?: inferLossless(
                    mimeType = finalMime,
                    nameOrUri = finalName
                ),
            audioTrackCount = extractorMetadata.audioTrackCount,
            extractorTrackCount = extractorMetadata.extractorTrackCount,
            audioTrackIndex = extractorMetadata.audioTrackIndex,
            pcmEncoding = extractorMetadata.pcmEncoding,
            maxInputSize = extractorMetadata.maxInputSize,
            encoderDelay = extractorMetadata.encoderDelay,
            encoderPadding = extractorMetadata.encoderPadding,
            language = extractorMetadata.language,
            profile = extractorMetadata.profile,
            level = extractorMetadata.level,
            metadataWarning =
            extractorMetadata.metadataWarning.ifBlank {
                retrieverMetadata.metadataWarning
            }
        )
    }

    private fun readWithExtractor(
        context: Context,
        uri: Uri,
        fallbackName: String,
        fileSizeBytes: Long,
        dateModifiedMillis: Long,
        sourceType: String,
        sourcePath: String
    ): AudioFormatMetadata {
        val extractor =
            MediaExtractor()

        return try {
            context.contentResolver.openFileDescriptor(
                uri,
                "r"
            )?.use { pfd ->
                extractor.setDataSource(
                    pfd.fileDescriptor
                )
            } ?: return AudioFormatMetadata(
                metadataWarning = "Could not open file descriptor for MediaExtractor."
            )

            var audioTrackCount =
                0

            var firstAudioTrackIndex =
                -1

            var firstAudioFormat: MediaFormat? =
                null

            for (
            index in 0 until extractor.trackCount
            ) {
                val format =
                    extractor.getTrackFormat(
                        index
                    )

                val mime =
                    getStringSafe(
                        format,
                        MediaFormat.KEY_MIME
                    )

                if (
                    mime.startsWith(
                        "audio/"
                    )
                ) {
                    audioTrackCount++

                    if (
                        firstAudioFormat == null
                    ) {
                        firstAudioFormat =
                            format

                        firstAudioTrackIndex =
                            index
                    }
                }
            }

            val format =
                firstAudioFormat
                    ?: return AudioFormatMetadata(
                        extractorTrackCount = extractor.trackCount,
                        audioTrackCount = audioTrackCount,
                        metadataWarning = "No audio track found by MediaExtractor."
                    )

            val mime =
                getStringSafe(
                    format,
                    MediaFormat.KEY_MIME
                )

            val finalName =
                fallbackName.ifBlank {
                    sourcePath.ifBlank {
                        uri.toString()
                    }
                }

            AudioFormatMetadata(
                audioCodec = codecLabelFromMimeOrName(
                    mimeType = mime,
                    nameOrUri = finalName
                ),
                mimeType = mime,
                bitrate = getIntSafe(
                    format,
                    MediaFormat.KEY_BIT_RATE
                ),
                sampleRate = getIntSafe(
                    format,
                    MediaFormat.KEY_SAMPLE_RATE
                ),
                bitDepth = getIntSafe(
                    format,
                    KEY_BITS_PER_SAMPLE
                ),
                channelCount = getIntSafe(
                    format,
                    MediaFormat.KEY_CHANNEL_COUNT
                ),
                fileSizeBytes = fileSizeBytes,
                dateModifiedMillis = dateModifiedMillis,
                container = containerFromNameOrMime(
                    nameOrUri = finalName,
                    mimeType = mime
                ),
                fileExtension = extensionFromNameOrUri(
                    finalName
                ),
                sourceType = sourceType,
                sourcePath = sourcePath,
                lossless = inferLossless(
                    mimeType = mime,
                    nameOrUri = finalName
                ),
                audioTrackCount = audioTrackCount,
                extractorTrackCount = extractor.trackCount,
                audioTrackIndex = firstAudioTrackIndex,
                pcmEncoding = getIntSafe(
                    format,
                    MediaFormat.KEY_PCM_ENCODING
                ),
                maxInputSize = getIntSafe(
                    format,
                    MediaFormat.KEY_MAX_INPUT_SIZE
                ),
                encoderDelay = getIntSafe(
                    format,
                    KEY_ENCODER_DELAY
                ),
                encoderPadding = getIntSafe(
                    format,
                    KEY_ENCODER_PADDING
                ),
                language = getStringSafe(
                    format,
                    MediaFormat.KEY_LANGUAGE
                ),
                profile = getIntSafe(
                    format,
                    MediaFormat.KEY_PROFILE
                ),
                level = getIntSafe(
                    format,
                    MediaFormat.KEY_LEVEL
                )
            )
        } catch (e: Exception) {
            AudioFormatMetadata(
                metadataWarning = "MediaExtractor failed: ${e.javaClass.simpleName}"
            )
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun readWithRetriever(
        context: Context,
        uri: Uri,
        fallbackName: String,
        fileSizeBytes: Long,
        dateModifiedMillis: Long,
        sourceType: String,
        sourcePath: String
    ): AudioFormatMetadata {
        val retriever =
            MediaMetadataRetriever()

        return try {
            retriever.setDataSource(
                context,
                uri
            )

            val mime =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                ).orEmpty()

            val finalName =
                fallbackName.ifBlank {
                    sourcePath.ifBlank {
                        uri.toString()
                    }
                }

            AudioFormatMetadata(
                audioCodec = codecLabelFromMimeOrName(
                    mimeType = mime,
                    nameOrUri = finalName
                ),
                mimeType = mime,
                bitrate =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_BITRATE
                )?.toIntOrNull()
                    ?: 0,
                sampleRate =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
                    )?.toIntOrNull() ?: 0
                } else {
                    0
                },
                bitDepth =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE
                )?.toIntOrNull()
                    ?: 0,
                genre =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_GENRE
                )?.trim()
                    .orEmpty(),
                fileSizeBytes = fileSizeBytes,
                dateModifiedMillis = dateModifiedMillis,
                container = containerFromNameOrMime(
                    nameOrUri = finalName,
                    mimeType = mime
                ),
                fileExtension = extensionFromNameOrUri(
                    finalName
                ),
                sourceType = sourceType,
                sourcePath = sourcePath,
                lossless = inferLossless(
                    mimeType = mime,
                    nameOrUri = finalName
                )
            )
        } catch (e: Exception) {
            AudioFormatMetadata(
                metadataWarning = "MediaMetadataRetriever failed: ${e.javaClass.simpleName}"
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun getStringSafe(
        format: MediaFormat,
        key: String
    ): String {
        return try {
            if (
                format.containsKey(
                    key
                )
            ) {
                format.getString(
                    key
                ).orEmpty()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun getIntSafe(
        format: MediaFormat,
        key: String
    ): Int {
        return try {
            if (
                format.containsKey(
                    key
                )
            ) {
                format.getInteger(
                    key
                )
            } else {
                0
            }
        } catch (_: Exception) {
            0
        }
    }
}