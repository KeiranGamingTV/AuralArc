package com.example.auralarc.navidrome

data class NavidromeApiResult(
    val success: Boolean,
    val message: String,
    val rawJson: String? = null
)