package com.nextcode.minichat.data.repositories

import com.nextcode.minichat.data.local.UserDao
import com.nextcode.minichat.data.local.UserEntity
import com.nextcode.minichat.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.nextcode.minichat.data.local.TokenManager
import com.nextcode.minichat.data.remote.LoginRequest
import com.nextcode.minichat.data.remote.UserDto
import com.nextcode.minichat.domain.repositories.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : UserRepository {

    override val currentUser: Flow<UserEntity?> = userDao.getCurrentUser()


    override suspend fun refreshUser() {
        try {
            if (tokenManager.getToken() == null) {
                performLogin()
            }
            
            val response = apiService.getMe()
            if (response.isSuccessful) {
                saveUser(response.body()?.data)
            } else if (response.code() == 401 || response.code() == 403) {
                // Token expired or invalid, try re-login
                performLogin()
                val retryResponse = apiService.getMe()
                if (retryResponse.isSuccessful) {
                    saveUser(retryResponse.body()?.data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun performLogin() {
        var deviceId = tokenManager.getDeviceId()
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            tokenManager.saveDeviceId(deviceId)
        }
        
        val loginResponse = apiService.login(LoginRequest(deviceId))
        if (loginResponse.isSuccessful) {
            loginResponse.body()?.data?.let {
                tokenManager.saveToken(it.accessToken)
            }
        }
    }

    private suspend fun saveUser(body: UserDto?) {
        if (body != null) {
            val country = com.nextcode.minichat.data.StaticData.getCountries().find { it.code == body.countryCode }
            val user = UserEntity(
                id = "me", // Consistency with UserDao query
                username = body.username,
                email = body.email ?: "",
                karma = body.karma,
                diamonds = body.diamonds,
                countryCode = body.countryCode,
                countryNameRes = country?.nameRes ?: com.nextcode.minichat.R.string.select_country,
                countryFlag = country?.flag ?: "🏳️",
                gender = body.gender,
                isPremium = body.isPremium
            )
            userDao.insertUser(user)
        }
    }

    override suspend fun updateGender(gender: String) {
        try {
            val response = apiService.updateGender(mapOf("gender" to gender))
            if (response.isSuccessful) {
                refreshUser()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
