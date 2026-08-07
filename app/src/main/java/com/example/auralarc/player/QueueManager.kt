package com.example.auralarc.player

import androidx.compose.runtime.mutableStateOf
import com.example.auralarc.data.MusicTrack
import com.example.auralarc.data.TrackIdentity
import java.util.UUID

data class QueueShuffleSnapshot(
    val sourceEntries: List<QueueEntry>,
    val currentEntry: QueueEntry
)

object QueueManager {

    val revision =
        mutableStateOf(
            0L
        )

    private var suppressCurrentIndexRevision =
        false

    private var queue =
        mutableListOf<QueueEntry>()

    private var normalQueue =
        mutableListOf<QueueEntry>()

    var currentIndex =
        -1
        private set(value) {
            if (
                field != value
            ) {
                field =
                    value

                if (
                    !suppressCurrentIndexRevision
                ) {
                    notifyQueueChanged()
                }
            }
        }

    private var addToQueueInsertIndex =
        -1

    private var normalAddToQueueInsertIndex =
        -1

    private fun notifyQueueChanged() {
        revision.value +=
            1L
    }

    private fun resetAddToQueueInsertIndex() {
        addToQueueInsertIndex =
            -1

        normalAddToQueueInsertIndex =
            -1
    }

    private fun createEntry(
        track: MusicTrack
    ): QueueEntry {
        return QueueEntry(
            entryId = UUID.randomUUID().toString(),
            track = track
        )
    }

    private fun createEntries(
        tracks: List<MusicTrack>
    ): List<QueueEntry> {
        return tracks.map { track ->
            createEntry(
                track
            )
        }
    }

    fun setQueue(
        tracks: List<MusicTrack>,
        startTrack: MusicTrack
    ) {
        resetAddToQueueInsertIndex()

        if (
            tracks.isEmpty()
        ) {
            clearQueue()
            return
        }

        val entries =
            createEntries(
                tracks
            )

        val startEntry =
            entries.firstOrNull { entry ->
                TrackIdentity.getStableId(
                    entry.track
                ) ==
                        TrackIdentity.getStableId(
                            startTrack
                        )
            } ?: entries.first()

        normalQueue.clear()

        normalQueue.addAll(
            entries
        )

        queue.clear()

        if (
            PlaybackState.shuffleEnabled.value
        ) {
            queue.add(
                startEntry
            )

            val remaining =
                entries.filterNot { entry ->
                    entry.entryId ==
                            startEntry.entryId
                }

            queue.addAll(
                SmartShuffleState.smartShuffleEntries(
                    remaining
                )
            )

            currentIndex =
                0
        } else {
            queue.addAll(
                entries
            )

            currentIndex =
                queue.indexOfFirst { entry ->
                    entry.entryId ==
                            startEntry.entryId
                }
        }

        notifyQueueChanged()
    }

    fun setQueuePreservingOrder(
        tracks: List<MusicTrack>,
        startTrack: MusicTrack
    ) {
        resetAddToQueueInsertIndex()

        if (
            tracks.isEmpty()
        ) {
            clearQueue()
            return
        }

        val entries =
            createEntries(
                tracks
            )

        val startEntry =
            entries.firstOrNull { entry ->
                TrackIdentity.getStableId(
                    entry.track
                ) ==
                        TrackIdentity.getStableId(
                            startTrack
                        )
            } ?: entries.first()

        normalQueue.clear()

        normalQueue.addAll(
            entries
        )

        queue.clear()

        queue.addAll(
            entries
        )

        currentIndex =
            queue.indexOfFirst { entry ->
                entry.entryId ==
                        startEntry.entryId
            }

        notifyQueueChanged()
    }

    fun rebuildForCurrentShuffleState(
        currentTrack: MusicTrack?
    ) {
        if (
            currentTrack == null
        ) {
            return
        }

        if (
            normalQueue.isEmpty()
        ) {
            normalQueue.addAll(
                queue
            )
        }

        if (
            normalQueue.isEmpty()
        ) {
            return
        }

        val currentEntry =
            currentEntry()
                ?: normalQueue.firstOrNull { entry ->
                    TrackIdentity.getStableId(
                        entry.track
                    ) ==
                            TrackIdentity.getStableId(
                                currentTrack
                            )
                }
                ?: return

        if (
            PlaybackState.shuffleEnabled.value
        ) {
            val remainingEntries =
                normalQueue.filterNot { entry ->
                    entry.entryId ==
                            currentEntry.entryId
                }

            val shuffledEntries =
                SmartShuffleState.smartShuffleEntries(
                    remainingEntries
                )

            queue.clear()

            queue.add(
                currentEntry
            )

            queue.addAll(
                shuffledEntries
            )

            currentIndex =
                0
        } else {
            queue.clear()

            queue.addAll(
                normalQueue
            )

            currentIndex =
                queue.indexOfFirst { entry ->
                    entry.entryId ==
                            currentEntry.entryId
                }

            if (
                currentIndex < 0 &&
                queue.isNotEmpty()
            ) {
                currentIndex =
                    0
            }
        }

        resetAddToQueueInsertIndex()

        notifyQueueChanged()
    }

    fun createShuffleSnapshot(
        currentTrack: MusicTrack?
    ): QueueShuffleSnapshot? {
        if (
            currentTrack == null
        ) {
            return null
        }

        if (
            normalQueue.isEmpty() &&
            queue.isNotEmpty()
        ) {
            normalQueue.addAll(
                queue
            )
        }

        if (
            normalQueue.isEmpty()
        ) {
            return null
        }

        val activeEntry =
            currentEntry()
                ?: normalQueue.firstOrNull { entry ->
                    TrackIdentity.getStableId(
                        entry.track
                    ) ==
                            TrackIdentity.getStableId(
                                currentTrack
                            )
                }
                ?: return null

        return QueueShuffleSnapshot(
            sourceEntries =
            normalQueue.toList(),
            currentEntry =
            activeEntry
        )
    }

    fun applyPreparedShuffleOrder(
        preparedEntries: List<QueueEntry>,
        currentEntryId: String
    ) {
        if (
            preparedEntries.isEmpty()
        ) {
            return
        }

        queue.clear()

        queue.addAll(
            preparedEntries
        )

        val newCurrentIndex =
            queue.indexOfFirst { entry ->
                entry.entryId ==
                        currentEntryId
            }
                .takeIf { index ->
                    index >= 0
                }
                ?: 0

        suppressCurrentIndexRevision =
            true

        try {
            currentIndex =
                newCurrentIndex
        } finally {
            suppressCurrentIndexRevision =
                false
        }

        resetAddToQueueInsertIndex()

        notifyQueueChanged()
    }

    fun replaceTracks(
        updatedTracks: List<MusicTrack>
    ) {
        if (
            updatedTracks.isEmpty()
        ) {
            return
        }

        val replacements =
            updatedTracks.associateBy { track ->
                track.uri
            }

        var changed =
            false

        for (
        index in queue.indices
        ) {
            val entry =
                queue[index]

            val replacement =
                replacements[entry.track.uri]
                    ?: continue

            queue[index] =
                entry.copy(
                    track = replacement
                )

            changed =
                true
        }

        for (
        index in normalQueue.indices
        ) {
            val entry =
                normalQueue[index]

            val replacement =
                replacements[entry.track.uri]
                    ?: continue

            normalQueue[index] =
                entry.copy(
                    track = replacement
                )

            changed =
                true
        }

        if (
            changed
        ) {
            notifyQueueChanged()
        }
    }

    fun currentTrack(): MusicTrack? {
        return queue
            .getOrNull(
                currentIndex
            )
            ?.track
    }

    fun currentEntry(): QueueEntry? {
        return queue.getOrNull(
            currentIndex
        )
    }

    fun nextTrack(
        fromAuto: Boolean = false
    ): MusicTrack? {
        if (
            queue.isEmpty()
        ) {
            return null
        }

        if (
            currentIndex !in queue.indices
        ) {
            currentIndex =
                0
        }

        if (
            fromAuto &&
            PlaybackState.repeatMode.value ==
            AuralArcRepeatMode.ONE
        ) {
            return currentTrack()
        }

        if (
            currentIndex <
            queue.lastIndex
        ) {
            currentIndex++

            resetAddToQueueInsertIndex()

            return currentTrack()
        }

        if (
            PlaybackState.repeatMode.value ==
            AuralArcRepeatMode.ALL
        ) {
            currentIndex =
                0

            resetAddToQueueInsertIndex()

            return currentTrack()
        }

        return null
    }

    fun previousTrack(): MusicTrack? {
        if (
            queue.isEmpty()
        ) {
            return null
        }

        if (
            currentIndex > 0
        ) {
            currentIndex--

            resetAddToQueueInsertIndex()

            return currentTrack()
        }

        if (
            PlaybackState.repeatMode.value ==
            AuralArcRepeatMode.ALL
        ) {
            currentIndex =
                queue.lastIndex

            resetAddToQueueInsertIndex()

            return currentTrack()
        }

        return null
    }

    fun addToQueue(
        track: MusicTrack
    ) {
        val entry =
            createEntry(
                track
            )

        if (
            queue.isEmpty()
        ) {
            queue.add(
                entry
            )

            normalQueue.add(
                entry
            )

            currentIndex =
                0

            addToQueueInsertIndex =
                1

            normalAddToQueueInsertIndex =
                1

            notifyQueueChanged()

            return
        }

        val visibleInsertIndex =
            if (
                addToQueueInsertIndex in
                0..queue.size
            ) {
                addToQueueInsertIndex
            } else {
                (
                        currentIndex + 1
                        ).coerceIn(
                        0,
                        queue.size
                    )
            }

        queue.add(
            visibleInsertIndex,
            entry
        )

        if (
            visibleInsertIndex <= currentIndex
        ) {
            currentIndex++
        }

        addToQueueInsertIndex =
            visibleInsertIndex + 1

        val currentEntry =
            currentEntry()

        val normalCurrentIndex =
            currentEntry?.let { activeEntry ->
                normalQueue.indexOfFirst { normalEntry ->
                    normalEntry.entryId ==
                            activeEntry.entryId
                }
            } ?: -1

        val normalInsertIndex =
            if (
                normalAddToQueueInsertIndex in
                0..normalQueue.size
            ) {
                normalAddToQueueInsertIndex
            } else {
                (
                        normalCurrentIndex + 1
                        ).coerceIn(
                        0,
                        normalQueue.size
                    )
            }

        normalQueue.add(
            normalInsertIndex,
            entry
        )

        normalAddToQueueInsertIndex =
            normalInsertIndex + 1

        notifyQueueChanged()
    }

    fun playNext(
        track: MusicTrack
    ) {
        val entry =
            createEntry(
                track
            )

        if (
            queue.isEmpty()
        ) {
            addToQueue(
                track
            )

            return
        }

        val insertIndex =
            (
                    currentIndex + 1
                    ).coerceIn(
                    0,
                    queue.size
                )

        queue.add(
            insertIndex,
            entry
        )

        if (
            insertIndex <= currentIndex
        ) {
            currentIndex++
        }

        val currentEntry =
            currentEntry()

        val normalCurrentIndex =
            currentEntry?.let { activeEntry ->
                normalQueue.indexOfFirst { normalEntry ->
                    normalEntry.entryId ==
                            activeEntry.entryId
                }
            } ?: -1

        val normalInsertIndex =
            (
                    normalCurrentIndex + 1
                    ).coerceIn(
                    0,
                    normalQueue.size
                )

        normalQueue.add(
            normalInsertIndex,
            entry
        )

        addToQueueInsertIndex =
            insertIndex + 1

        normalAddToQueueInsertIndex =
            normalInsertIndex + 1

        notifyQueueChanged()
    }

    fun removeAt(
        index: Int
    ): Boolean {
        if (
            index !in queue.indices
        ) {
            return false
        }

        val removedEntry =
            queue[index]

        val removedCurrent =
            index == currentIndex

        queue.removeAt(
            index
        )

        normalQueue.removeAll { entry ->
            entry.entryId ==
                    removedEntry.entryId
        }

        when {
            queue.isEmpty() -> {
                currentIndex =
                    -1
            }

            removedCurrent -> {
                currentIndex =
                    index.coerceAtMost(
                        queue.lastIndex
                    )
            }

            index < currentIndex -> {
                currentIndex--
            }
        }

        resetAddToQueueInsertIndex()

        notifyQueueChanged()

        return removedCurrent
    }

    fun moveUp(
        index: Int
    ) {
        if (
            index <= 0 ||
            index !in queue.indices
        ) {
            return
        }

        val item =
            queue.removeAt(
                index
            )

        queue.add(
            index - 1,
            item
        )

        when (
            currentIndex
        ) {
            index ->
                currentIndex =
                    index - 1

            index - 1 ->
                currentIndex =
                    index
        }

        moveInNormalQueue(
            entryId = item.entryId,
            direction = -1
        )

        resetAddToQueueInsertIndex()

        notifyQueueChanged()
    }

    fun moveDown(
        index: Int
    ) {
        if (
            index < 0 ||
            index >= queue.lastIndex
        ) {
            return
        }

        val item =
            queue.removeAt(
                index
            )

        queue.add(
            index + 1,
            item
        )

        when (
            currentIndex
        ) {
            index ->
                currentIndex =
                    index + 1

            index + 1 ->
                currentIndex =
                    index
        }

        moveInNormalQueue(
            entryId = item.entryId,
            direction = 1
        )

        resetAddToQueueInsertIndex()

        notifyQueueChanged()
    }

    private fun moveInNormalQueue(
        entryId: String,
        direction: Int
    ) {
        val oldIndex =
            normalQueue.indexOfFirst { entry ->
                entry.entryId ==
                        entryId
            }

        if (
            oldIndex !in normalQueue.indices
        ) {
            return
        }

        val newIndex =
            (
                    oldIndex + direction
                    ).coerceIn(
                    0,
                    normalQueue.lastIndex
                )

        if (
            newIndex ==
            oldIndex
        ) {
            return
        }

        val item =
            normalQueue.removeAt(
                oldIndex
            )

        normalQueue.add(
            newIndex,
            item
        )
    }

    fun clearQueue() {
        queue.clear()

        normalQueue.clear()

        currentIndex =
            -1

        resetAddToQueueInsertIndex()

        notifyQueueChanged()
    }

    fun getQueue(): List<MusicTrack> {
        return queue.map { entry ->
            entry.track
        }
    }

    fun getQueueEntries(): List<QueueEntry> {
        return queue.toList()
    }

    fun getNormalQueue(): List<MusicTrack> {
        return normalQueue.map { entry ->
            entry.track
        }
    }

    fun getNormalQueueEntries(): List<QueueEntry> {
        return normalQueue.toList()
    }

    fun setCurrentQueueIndex(
        index: Int
    ) {
        if (
            index in queue.indices
        ) {
            val previousEntryId =
                currentEntry()?.entryId

            val nextEntryId =
                queue
                    .getOrNull(
                        index
                    )
                    ?.entryId

            currentIndex =
                index

            if (
                previousEntryId != nextEntryId
            ) {
                resetAddToQueueInsertIndex()
            }
        }
    }

    fun playFromQueue(
        index: Int
    ): MusicTrack? {
        if (
            index !in queue.indices
        ) {
            return null
        }

        currentIndex =
            index

        resetAddToQueueInsertIndex()

        return currentTrack()
    }

    fun playFromQueueEntry(
        entryId: String
    ): MusicTrack? {
        val index =
            queue.indexOfFirst { entry ->
                entry.entryId ==
                        entryId
            }

        if (
            index < 0
        ) {
            return null
        }

        currentIndex =
            index

        resetAddToQueueInsertIndex()

        return currentTrack()
    }

    fun getCurrentEntryId(): String? {
        return currentEntry()?.entryId
    }

    fun setQueueFromUris(
        allTracks: List<MusicTrack>,
        queueUris: List<String>,
        startUri: String
    ): MusicTrack? {
        val rebuiltQueue =
            queueUris.mapNotNull { uri ->
                allTracks.firstOrNull { track ->
                    track.uri ==
                            uri
                }
            }

        if (
            rebuiltQueue.isEmpty()
        ) {
            return null
        }

        val startTrack =
            rebuiltQueue.firstOrNull { track ->
                track.uri ==
                        startUri
            } ?: rebuiltQueue.first()

        setQueuePreservingOrder(
            rebuiltQueue,
            startTrack
        )

        return startTrack
    }
}