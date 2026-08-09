package com.keiranhaas.auralarc.data

import java.io.Serializable

data class Playlist(
    val id: String,
    val name: String,
    val source: String = "LOCAL",
    val remoteId: String? = null,
    val trackKeys: List<String> = emptyList(),
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable