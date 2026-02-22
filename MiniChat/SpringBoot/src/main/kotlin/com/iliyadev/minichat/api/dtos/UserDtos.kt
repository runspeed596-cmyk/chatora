package com.iliyadev.minichat.api.dtos

data class UserDto(
    val id: String,
    val username: String,
    val email: String?,
    val googleId: String?,
    val karma: Int,
    val diamonds: Int,
    val countryCode: String?,
    val languageCode: String?,
    val gender: String,
    val isPremium: Boolean,
    val isBanned: Boolean
)
