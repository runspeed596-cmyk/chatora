package com.nextcode.minichat.data.remote

data class UserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val googleId: String? = null,
    val karma: Int,
    val diamonds: Int,
    val countryCode: String,
    val languageCode: String? = "en",
    val gender: String = "UNSPECIFIED",
    val isPremium: Boolean = false,
    val isBanned: Boolean = false
)
