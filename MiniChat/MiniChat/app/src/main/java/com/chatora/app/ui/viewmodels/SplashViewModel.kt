package com.chatora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.chatora.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    fun isOnboardingSeen(): Boolean = tokenManager.isOnboardingSeen()

    fun isLoggedIn(): Boolean = tokenManager.getToken() != null

    fun hasPermissions(): Boolean {
        // Permissions check logic if needed, but AppNavigation/PermissionScreen handles it
        return true 
    }
}
