package com.nextcode.minichat.domain.repositories

import com.nextcode.minichat.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val currentUser: Flow<UserEntity?>
    suspend fun refreshUser()
    suspend fun updateGender(gender: String)
}
