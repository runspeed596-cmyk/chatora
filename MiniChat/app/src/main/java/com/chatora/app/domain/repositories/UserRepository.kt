package com.chatora.app.domain.repositories

import com.chatora.app.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUser: Flow<UserEntity?>
    suspend fun refreshUser()
    suspend fun updateGender(gender: String)
}
