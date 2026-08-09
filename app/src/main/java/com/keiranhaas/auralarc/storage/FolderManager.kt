package com.keiranhaas.auralarc.storage

object FolderManager {

    val allowedFolders =
        listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/AuralArc"
        )

    var folders =
        allowedFolders.toMutableList()

}