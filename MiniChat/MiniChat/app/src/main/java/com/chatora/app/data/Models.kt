package com.chatora.app.data

import androidx.annotation.StringRes

data class Country(
    val code: String,
    @StringRes val nameRes: Int,
    val flag: String
)

data class User(
    val id: String,
    val username: String,
    val email: String,
    val karma: Int = 0,
    val diamonds: Int = 0,
    val country: Country? = null,
    val gender: String = "UNSPECIFIED",
    val isPremium: Boolean = false
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}
