package com.keiranhaas.auralarc.ui

enum class TrackSortMode(
    val label: String
) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    RELEASE_DATE("Release Date");

    fun next(): TrackSortMode {
        val allModes =
            values()

        val nextIndex =
            (ordinal + 1) % allModes.size

        return allModes[nextIndex]
    }
}