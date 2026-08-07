package com.example.auralarc.data

import java.io.Serializable

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val genre: String = "",
    val duration: Long = 0L,
    val uri: String,
    val albumId: Long,
    val trackNumber: Int,
    val albumArtPath: String? = null,
    val releaseDate: String = "",
    val releaseYear: Int = 0,
    val stableTrackId: String = "",


    val audioCodec: String = "",
    val mimeType: String = "",
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val channelCount: Int = 0,

    val fileSizeBytes: Long = 0L,
    val dateAddedMillis: Long = 0L,
    val dateModifiedMillis: Long = 0L,
    val discNumber: Int = 0,

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

    val replayGainTrackGain: Float? = null,
    val replayGainAlbumGain: Float? = null,
    val replayGainTrackPeak: Float? = null,
    val replayGainAlbumPeak: Float? = null,

    val metadataWarning: String = ""
) : Serializable {
    val isHighResolution: Boolean
        get() = bitDepth >= 24
}