package com.chatora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.chatora.app.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    fun completeOnboarding() {
        tokenManager.saveOnboardingSeen(true)
    }
}
