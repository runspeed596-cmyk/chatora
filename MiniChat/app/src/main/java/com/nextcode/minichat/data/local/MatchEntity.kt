package com.nextcode.minichat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val partnerId: String,
    val partnerUsername: String,
    val partnerCountry: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)
