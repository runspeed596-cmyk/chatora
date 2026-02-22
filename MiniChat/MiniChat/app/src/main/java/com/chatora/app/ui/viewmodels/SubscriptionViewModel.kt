package com.chatora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatora.app.data.models.CreatePaymentResponseDto
import com.chatora.app.data.models.SubscriptionPlanDto
import com.chatora.app.data.models.SubscriptionStatusDto
import com.chatora.app.data.repositories.SubscriptionRepository
import com.chatora.app.domain.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SubscriptionUiState {
    object Idle : SubscriptionUiState()
    object Loading : SubscriptionUiState()
    data class Success(val plans: List<SubscriptionPlanDto>, val status: SubscriptionStatusDto) : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl = _paymentUrl.asStateFlow()

    private val _activePlan = MutableStateFlow<String?>(null)
    val activePlan = _activePlan.asStateFlow()

    private val _showSuccess = MutableStateFlow(false)
    val showSuccess = _showSuccess.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            repository.getPlans().collect { plansResult ->
                repository.getSubscriptionStatus().collect { statusResult ->
                    if (plansResult.isSuccess && statusResult.isSuccess) {
                        userRepository.refreshUser()
                        val status = statusResult.getOrThrow()
                        _activePlan.value = status.plan
                        _uiState.value = SubscriptionUiState.Success(plansResult.getOrThrow(), status)
                    } else {
                        _uiState.value = SubscriptionUiState.Error("Failed to load subscription data")
                    }
                }
            }
        }
    }

    fun startPayment(plan: String) {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            val result = repository.createPayment(plan)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                if (data.paymentLink == "SUCCESS") {
                    userRepository.refreshUser()
                    _showSuccess.value = true
                    loadData()
                } else {
                    _paymentUrl.value = data.paymentLink
                }
            } else {
                _uiState.value = SubscriptionUiState.Error("Failed to initiate payment")
            }
        }
    }

    fun clearPaymentUrl() {
        _paymentUrl.value = null
    }

    fun dismissSuccess() {
        _showSuccess.value = false
    }
}
