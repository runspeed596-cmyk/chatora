package com.nextcode.minichat.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nextcode.minichat.data.local.TokenManager
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
