package com.chatora.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val karma: Int,
    val diamonds: Int,
    val countryCode: String,
    val countryNameRes: Int,
    val countryFlag: String,
    val gender: String = "UNSPECIFIED",
    val isPremium: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
