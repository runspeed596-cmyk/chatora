package com.chatora.app.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

// Reusing same data classes for now, usually these would be Domain Models
// data class Country(val code: String, val name: String, val flag: String)
// data class User(...)

@Singleton
class MockDataRepository @Inject constructor() {
    
    val currentUser = User(
        id = "me",
        username = "MyUser",
        email = "user@example.com",
        karma = 120,
        diamonds = 50,
        country = Country("US", com.chatora.app.R.string.country_us, "🇺🇸"),
        isPremium = true
    )

    // Use Static Data now
    val countries = StaticData.getCountries()

    suspend fun findMatch(): User? {
        delay(3000) // Simulate network delay
        return if (Math.random() > 0.1) {
            User(
                id = "partner_${System.currentTimeMillis()}",
                username = "RandomPartner_${(100..999).random()}",
                email = "partner@example.com",
                karma = (50..500).random(),
                diamonds = 0,
                country = countries.random()
            )
        } else {
            null // Simulate failure occasionally
        }
    }
}
