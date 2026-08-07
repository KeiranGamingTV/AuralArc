package com.example.auralarc.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.auralarc.data.MusicTrack
import java.io.File
import android.provider.DocumentsContract
import java.util.concurrent.ConcurrentHashMap

object LrcLyricsFinder {

    private sealed class IndexedLyricsFile {

        data class LocalFile(
            val file: File
        ) : IndexedLyricsFile()

        data class DocumentUri(
            val uri: Uri
        ) : IndexedLyricsFile()
    }

    private data class LyricsIndex(
        val regularLyrics:
        Map<String, IndexedLyricsFile>,
        val duetLyrics:
        Map<String, IndexedLyricsFile>
    )

    @Volatile
    private var cachedIndex: LyricsIndex? =
        null

    private val indexLock =
        Any()

    private val whitespaceRegex =
        Regex(
            """\s+"""
        )

    fun invalidateIndex() {
        synchronized(
            indexLock
        ) {
            cachedIndex =
                null
        }
    }

    fun findLyricsForTrack(
        context: Context,
        track: MusicTrack
    ): EmbeddedLyricsResult? {
        return findSidecarLyrics(
            context = context,
            track = track,
            extension = "lrc",
            lyricsType = EmbeddedLyricsType.SYNCED
        )
    }

    fun findDuetLyricsForTrack(
        context: Context,
        track: MusicTrack
    ): EmbeddedLyricsResult? {
        return findSidecarLyrics(
            context = context,
            track = track,
            extension = "dlrc",
            lyricsType = EmbeddedLyricsType.DUET_SYNCED
        )
    }

    private fun findSidecarLyrics(
        context: Context,
        track: MusicTrack,
        extension: String,
        lyricsType: EmbeddedLyricsType
    ): EmbeddedLyricsResult? {
        val candidateNames =
            buildCandidateNames(
                context = context,
                track = track
            )

        if (
            candidateNames.isEmpty()
        ) {
            return null
        }

        val sameDocumentFolderResult =
            findInSameDocumentFolderAsAudioFile(
                context = context,
                track = track,
                normalizedCandidateNames = candidateNames,
                extension = extension,
                lyricsType = lyricsType
            )

        if (
            sameDocumentFolderResult != null
        ) {
            return sameDocumentFolderResult
        }

        val sameFolderResult =
            findInSameFolderAsAudioFile(
                context = context,
                track = track,
                normalizedCandidateNames = candidateNames,
                extension = extension,
                lyricsType = lyricsType
            )

        if (
            sameFolderResult != null
        ) {
            return sameFolderResult
        }

        return findInIndexedFolders(
            context =
                context,
            normalizedCandidateNames =
                candidateNames,
            extension =
                extension,
            lyricsType =
                lyricsType
        )
    }

    private fun findInIndexedFolders(
        context: Context,
        normalizedCandidateNames: Set<String>,
        extension: String,
        lyricsType: EmbeddedLyricsType
    ): EmbeddedLyricsResult? {
        val index =
            getOrBuildIndex(
                context.applicationContext
            )

        val sourceMap =
            if (
                extension.equals(
                    "dlrc",
                    ignoreCase = true
                )
            ) {
                index.duetLyrics
            } else {
                index.regularLyrics
            }

        normalizedCandidateNames.forEach { candidate ->
            val indexedFile =
                sourceMap[candidate]
                    ?: return@forEach

            val text =
                when (
                    indexedFile
                ) {
                    is IndexedLyricsFile.LocalFile -> {
                        readFileTextSafely(
                            indexedFile.file
                        )
                    }

                    is IndexedLyricsFile.DocumentUri -> {
                        readDocumentTextSafely(
                            context =
                                context,
                            uri =
                                indexedFile.uri
                        )
                    }
                }

            if (
                text.isNotBlank()
            ) {
                val source =
                    when (
                        indexedFile
                    ) {
                        is IndexedLyricsFile.LocalFile ->
                            indexedFile
                                .file
                                .absolutePath

                        is IndexedLyricsFile.DocumentUri ->
                            indexedFile
                                .uri
                                .toString()
                    }

                return EmbeddedLyricsResult(
                    text =
                        text,
                    type =
                        lyricsType,
                    source =
                        source
                )
            }
        }

        return null
    }

    private fun buildCandidateNames(
        context: Context,
        track: MusicTrack
    ): Set<String> {
        val names =
            linkedSetOf<String>()

        /*
         * Add the title stored in MusicTrack. Depending on the
         * audio file's metadata, this may be either the real song
         * title or the full filename.
         */
        addCandidateNameVariants(
            destination = names,
            rawName = track.title
        )

        val audioFileName =
            getAudioFileBaseName(
                context = context,
                trackUri = track.uri
            )

        if (
            !audioFileName.isNullOrBlank()
        ) {
            addCandidateNameVariants(
                destination = names,
                rawName = audioFileName
            )
        }

        return names
    }

    private fun addCandidateNameVariants(
        destination: MutableSet<String>,
        rawName: String
    ) {
        val normalizedName =
            normalizeTitle(
                rawName
            )

        if (
            normalizedName.isBlank()
        ) {
            return
        }

        destination.add(
            normalizedName
        )

        /*
         * Remove a leading track number in forms such as:
         *
         * 02 - Song
         * 02. Song
         * 02_ Song
         * 02) Song
         * 02 Song
         */
        val withoutTrackNumber =
            normalizedName
                .replace(
                    Regex(
                        """^\s*\d{1,3}(?:\s*[-._)]\s*|\s+)"""
                    ),
                    ""
                )
                .trim()

        if (
            withoutTrackNumber.isNotBlank()
        ) {
            destination.add(
                withoutTrackNumber
            )
        }

        /*
         * Support common filename layouts such as:
         *
         * Artist - Song Title
         * 02 - Artist - Song Title
         *
         * The final segment becomes another possible sidecar name.
         */
        val filenameParts =
            withoutTrackNumber
                .split(
                    Regex(
                        """\s+[-–—]\s+"""
                    )
                )
                .map { part ->
                    part.trim()
                }
                .filter { part ->
                    part.isNotBlank()
                }

        if (
            filenameParts.size >= 2
        ) {
            destination.add(
                filenameParts.last()
            )
        }
    }

    private fun findInSameDocumentFolderAsAudioFile(
        context: Context,
        track: MusicTrack,
        normalizedCandidateNames: Set<String>,
        extension: String,
        lyricsType: EmbeddedLyricsType
    ): EmbeddedLyricsResult? {
        val audioUri =
            try {
                Uri.parse(
                    track.uri
                )
            } catch (_: Exception) {
                return null
            }

        if (
            !audioUri.scheme.equals(
                "content",
                ignoreCase = true
            )
        ) {
            return null
        }

        val isDocumentUri =
            try {
                DocumentsContract.isDocumentUri(
                    context,
                    audioUri
                )
            } catch (_: Exception) {
                false
            }

        if (
            !isDocumentUri
        ) {
            return null
        }

        val audioDocumentId =
            try {
                DocumentsContract.getDocumentId(
                    audioUri
                )
            } catch (_: Exception) {
                return null
            }

        /*
         * SAF document IDs generally resemble:
         *
         * primary:Music/Album/Song.flac
         *
         * Remove the last segment to get the real parent folder.
         */
        val lastSeparatorIndex =
            audioDocumentId.lastIndexOf(
                '/'
            )

        if (
            lastSeparatorIndex <= 0
        ) {
            return null
        }

        val parentDocumentId =
            audioDocumentId.substring(
                0,
                lastSeparatorIndex
            )

        val childrenUri =
            try {
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        audioUri,
                        parentDocumentId
                    )
            } catch (_: Exception) {
                return null
            }

        val projection =
            arrayOf(
                DocumentsContract.Document
                    .COLUMN_DOCUMENT_ID,
                DocumentsContract.Document
                    .COLUMN_DISPLAY_NAME
            )

        try {
            context.contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val documentIdIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document
                            .COLUMN_DOCUMENT_ID
                    )

                val displayNameIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document
                            .COLUMN_DISPLAY_NAME
                    )

                if (
                    documentIdIndex < 0 ||
                    displayNameIndex < 0
                ) {
                    return@use
                }

                while (
                    cursor.moveToNext()
                ) {
                    val childName =
                        cursor.getString(
                            displayNameIndex
                        ) ?: continue

                    if (
                        !childName.endsWith(
                            ".$extension",
                            ignoreCase = true
                        )
                    ) {
                        continue
                    }

                    val normalizedChildName =
                        normalizeSidecarFileName(
                            fileName = childName,
                            extension = extension
                        )

                    if (
                        normalizedChildName !in
                        normalizedCandidateNames
                    ) {
                        continue
                    }

                    val childDocumentId =
                        cursor.getString(
                            documentIdIndex
                        ) ?: continue

                    val childUri =
                        try {
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    audioUri,
                                    childDocumentId
                                )
                        } catch (_: Exception) {
                            continue
                        }

                    val text =
                        readDocumentTextSafely(
                            context = context,
                            uri = childUri
                        )

                    if (
                        text.isNotBlank()
                    ) {
                        return EmbeddedLyricsResult(
                            text = text,
                            type = lyricsType,
                            source = childUri.toString()
                        )
                    }
                }
            }
        } catch (_: Exception) {
            return null
        }

        return null
    }

    private fun findInSameFolderAsAudioFile(
        context: Context,
        track: MusicTrack,
        normalizedCandidateNames: Set<String>,
        extension: String,
        lyricsType: EmbeddedLyricsType
    ): EmbeddedLyricsResult? {
        val parentFolder =
            getAudioParentFolder(
                context = context,
                trackUri = track.uri
            ) ?: return null

        return findMatchingSidecarInFileFolder(
            folder = parentFolder,
            normalizedCandidateNames = normalizedCandidateNames,
            extension = extension,
            lyricsType = lyricsType
        )
    }

    private fun findMatchingSidecarInFileFolder(
        folder: File,
        normalizedCandidateNames: Set<String>,
        extension: String,
        lyricsType: EmbeddedLyricsType
    ): EmbeddedLyricsResult? {
        if (
            !folder.exists() ||
            !folder.isDirectory
        ) {
            return null
        }

        val files =
            folder.listFiles()
                ?: return null

        files.forEach { file ->
            if (
                file.isFile &&
                file.extension.equals(
                    extension,
                    ignoreCase = true
                ) &&
                normalizeSidecarFileName(
                    fileName = file.name,
                    extension = extension
                ) in normalizedCandidateNames
            ) {
                val text =
                    readFileTextSafely(
                        file
                    )

                if (
                    text.isNotBlank()
                ) {
                    return EmbeddedLyricsResult(
                        text = text,
                        type = lyricsType,
                        source = file.absolutePath
                    )
                }
            }
        }

        return null
    }

    private fun getAudioFileBaseName(
        context: Context,
        trackUri: String
    ): String? {
        return try {
            val uri =
                Uri.parse(
                    trackUri
                )

            when (
                uri.scheme?.lowercase()
            ) {
                "file" ->
                    File(
                        uri.path ?: return null
                    ).nameWithoutExtension

                "content" -> {
                    context.contentResolver.query(
                        uri,
                        arrayOf(
                            MediaStore.MediaColumns.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val index =
                            cursor.getColumnIndex(
                                MediaStore.MediaColumns.DISPLAY_NAME
                            )

                        if (
                            index >= 0 &&
                            cursor.moveToFirst()
                        ) {
                            cursor.getString(
                                index
                            )
                                ?.substringBeforeLast(
                                    ".",
                                    missingDelimiterValue = cursor.getString(
                                        index
                                    ).orEmpty()
                                )
                        } else {
                            null
                        }
                    }
                }

                null ->
                    File(
                        trackUri
                    ).nameWithoutExtension

                else ->
                    null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getAudioParentFolder(
        context: Context,
        trackUri: String
    ): File? {
        return try {
            val uri =
                Uri.parse(
                    trackUri
                )

            if (
                uri.scheme == "file"
            ) {
                return File(
                    uri.path ?: return null
                ).parentFile
            }

            if (
                uri.scheme == "content"
            ) {
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.MediaColumns.DATA
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val index =
                        cursor.getColumnIndex(
                            MediaStore.MediaColumns.DATA
                        )

                    if (
                        index >= 0 &&
                        cursor.moveToFirst()
                    ) {
                        val path =
                            cursor.getString(
                                index
                            )

                        if (
                            !path.isNullOrBlank()
                        ) {
                            return File(
                                path
                            ).parentFile
                        }
                    }
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun readFileTextSafely(
        file: File
    ): String {
        return try {
            if (
                file.length() > 2L * 1024L * 1024L
            ) {
                return ""
            }

            file.readText()
        } catch (_: Exception) {
            ""
        }
    }

    private fun readDocumentTextSafely(
        context: Context,
        uri: Uri
    ): String {
        val inputStreamText =
            try {
                context.contentResolver
                    .openInputStream(
                        uri
                    )
                    ?.bufferedReader(
                        Charsets.UTF_8
                    )
                    ?.use { reader ->
                        reader.readText()
                    }
                    .orEmpty()
            } catch (_: Exception) {
                ""
            }

        if (
            inputStreamText.isNotBlank()
        ) {
            return inputStreamText
        }

        /*
         * Some document providers fail openInputStream() for an
         * unfamiliar extension but still allow a readable file
         * descriptor.
         */
        return try {
            context.contentResolver
                .openFileDescriptor(
                    uri,
                    "r"
                )
                ?.use { descriptor ->
                    java.io.FileInputStream(
                        descriptor.fileDescriptor
                    )
                        .bufferedReader(
                            Charsets.UTF_8
                        )
                        .use { reader ->
                            reader.readText()
                        }
                }
                .orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun normalizeSidecarFileName(
        fileName: String,
        extension: String
    ): String {
        return normalizeTitle(
            fileName.removeSuffixIgnoreCase(
                ".$extension"
            )
        )
    }

    private fun normalizeTitle(
        value: String
    ): String {
        return value
            .replace(
                '’',
                '\''
            )
            .trim()
            .lowercase()
            .replace(
                whitespaceRegex,
                " "
            )
    }

    private fun String.removeSuffixIgnoreCase(
        suffix: String
    ): String {
        return if (
            endsWith(
                suffix,
                ignoreCase = true
            )
        ) {
            substring(
                0,
                length - suffix.length
            )
        } else {
            this
        }
    }

    private fun getOrBuildIndex(
        context: Context
    ): LyricsIndex {
        cachedIndex?.let { existing ->
            return existing
        }

        synchronized(
            indexLock
        ) {
            cachedIndex?.let { existing ->
                return existing
            }

            val regularLyrics =
                linkedMapOf<String, IndexedLyricsFile>()

            val duetLyrics =
                linkedMapOf<String, IndexedLyricsFile>()

            val defaultFolders =
                try {
                    FolderPreferences.loadFolders(
                        context
                    )
                } catch (_: Exception) {
                    FolderManager.allowedFolders
                        .toMutableList()
                }

            defaultFolders.forEach { folderPath ->
                indexLocalFolder(
                    folder =
                        File(
                            folderPath
                        ),
                    regularLyrics =
                        regularLyrics,
                    duetLyrics =
                        duetLyrics
                )
            }

            val pickedFolders =
                PickedFolderStore.getFolderUris(
                    context
                )

            pickedFolders.forEach { uriString ->
                val root =
                    try {
                        DocumentFile.fromTreeUri(
                            context,
                            Uri.parse(
                                uriString
                            )
                        )
                    } catch (_: Exception) {
                        null
                    }

                if (
                    root != null
                ) {
                    indexDocumentFolder(
                        folder =
                            root,
                        regularLyrics =
                            regularLyrics,
                        duetLyrics =
                            duetLyrics
                    )
                }
            }

            val result =
                LyricsIndex(
                    regularLyrics =
                        regularLyrics,
                    duetLyrics =
                        duetLyrics
                )

            cachedIndex =
                result

            return result
        }
    }

    private fun indexLocalFolder(
        folder: File,
        regularLyrics:
        MutableMap<String, IndexedLyricsFile>,
        duetLyrics:
        MutableMap<String, IndexedLyricsFile>
    ) {
        if (
            !folder.exists() ||
            !folder.isDirectory
        ) {
            return
        }

        val files =
            folder.listFiles()
                ?: return

        files.forEach { file ->
            when {
                file.isDirectory -> {
                    indexLocalFolder(
                        folder =
                            file,
                        regularLyrics =
                            regularLyrics,
                        duetLyrics =
                            duetLyrics
                    )
                }

                file.isFile &&
                        file.extension.equals(
                            "dlrc",
                            ignoreCase = true
                        ) -> {
                    val key =
                        normalizeSidecarFileName(
                            fileName =
                                file.name,
                            extension =
                                "dlrc"
                        )

                    if (
                        key.isNotBlank()
                    ) {
                        duetLyrics.putIfAbsent(
                            key,
                            IndexedLyricsFile.LocalFile(
                                file
                            )
                        )
                    }
                }

                file.isFile &&
                        file.extension.equals(
                            "lrc",
                            ignoreCase = true
                        ) -> {
                    val key =
                        normalizeSidecarFileName(
                            fileName =
                                file.name,
                            extension =
                                "lrc"
                        )

                    if (
                        key.isNotBlank()
                    ) {
                        regularLyrics.putIfAbsent(
                            key,
                            IndexedLyricsFile.LocalFile(
                                file
                            )
                        )
                    }
                }
            }
        }
    }

    private fun indexDocumentFolder(
        folder: DocumentFile,
        regularLyrics:
        MutableMap<String, IndexedLyricsFile>,
        duetLyrics:
        MutableMap<String, IndexedLyricsFile>
    ) {
        if (
            !folder.isDirectory
        ) {
            return
        }

        val children =
            try {
                folder.listFiles()
            } catch (_: Exception) {
                emptyArray()
            }

        children.forEach { child ->
            when {
                child.isDirectory -> {
                    indexDocumentFolder(
                        folder =
                            child,
                        regularLyrics =
                            regularLyrics,
                        duetLyrics =
                            duetLyrics
                    )
                }

                child.isFile -> {
                    val name =
                        child.name
                            .orEmpty()

                    when {
                        name.endsWith(
                            ".dlrc",
                            ignoreCase = true
                        ) -> {
                            val key =
                                normalizeSidecarFileName(
                                    fileName =
                                        name,
                                    extension =
                                        "dlrc"
                                )

                            if (
                                key.isNotBlank()
                            ) {
                                duetLyrics.putIfAbsent(
                                    key,
                                    IndexedLyricsFile.DocumentUri(
                                        child.uri
                                    )
                                )
                            }
                        }

                        name.endsWith(
                            ".lrc",
                            ignoreCase = true
                        ) -> {
                            val key =
                                normalizeSidecarFileName(
                                    fileName =
                                        name,
                                    extension =
                                        "lrc"
                                )

                            if (
                                key.isNotBlank()
                            ) {
                                regularLyrics.putIfAbsent(
                                    key,
                                    IndexedLyricsFile.DocumentUri(
                                        child.uri
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}