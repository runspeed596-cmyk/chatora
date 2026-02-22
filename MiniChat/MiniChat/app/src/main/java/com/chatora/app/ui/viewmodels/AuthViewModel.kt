package com.chatora.app.ui.viewmodels

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.chatora.app.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatora.app.data.remote.ApiException
import com.chatora.app.data.remote.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.GoogleSignIn -> signInWithGoogle(event.context)
            is AuthEvent.EmailSignIn -> signInWithEmail()
            is AuthEvent.EmailSignUp -> signUpWithEmail()
            is AuthEvent.VerifyEmail -> verifyEmail(event.code)
            is AuthEvent.ToggleMode -> _uiState.update { it.copy(isLoginMode = !it.isLoginMode) }
            is AuthEvent.EmailChanged -> _uiState.update { it.copy(email = event.email) }
            is AuthEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
            is AuthEvent.UsernameChanged -> _uiState.update { it.copy(username = event.username) }
            is AuthEvent.DismissError -> _uiState.update { it.copy(error = null) }
            is AuthEvent.ResetVerificationState -> _uiState.update { it.copy(needsVerification = false) }
            is AuthEvent.Logout -> logout()
            is AuthEvent.ResendCode -> resendCode()
        }
    }

    private var timerJob: kotlinx.coroutines.Job? = null

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.update { it.copy(resendTimer = 120) }
            while (_uiState.value.resendTimer > 0) {
                kotlinx.coroutines.delay(1000)
                _uiState.update { it.copy(resendTimer = it.resendTimer - 1) }
            }
        }
    }

    private fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val credentialManager = CredentialManager.create(context)

                val serverClientId = "1016064715588-04rdpfiiqht0id7vmgel3npipdc41cbo.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = try {
                    credentialManager.getCredential(context, request)
                } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                    _uiState.update { it.copy(isLoading = false, error = "No Google account found. Please add a Google account in device Settings.") }
                    return@launch
                } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = "Google Sign-In Error [${e::class.java.simpleName}]: ${e.message}") }
                    return@launch
                }

                val credential = result.credential

                val idToken = when {
                    credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential -> {
                        credential.idToken
                    }
                    credential is androidx.credentials.CustomCredential && 
                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        try {
                            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data).idToken
                        } catch (e: Exception) {
                            null
                        }
                    }
                    else -> null
                }

                if (idToken != null) {
                    authRepository.loginWithGoogle(idToken)
                        .onSuccess { 
                            _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                        }
                        . onFailure { error ->
                            val message = NetworkUtils.getErrorMessageFromException(context, error)
                            _uiState.update { it.copy(isLoading = false, error = message) }
                        }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Unexpected credential type: ${credential::class.java.simpleName}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Google Sign-In Exception: ${e.message}") }
            }
        }
    }

    private fun signInWithEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = _uiState.value.email.trim().lowercase()
            authRepository.loginWithEmail(email, _uiState.value.password)
                .onSuccess { response ->
                    if (response.emailVerified) {
                        _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, needsVerification = true) }
                        // Auto-send code if just logging in and needing verification
                        resendCode() 
                    }
                }
                .onFailure { error ->
                    if (error is ApiException && error.code == "EMAIL_NOT_VERIFIED") {
                        _uiState.update { it.copy(isLoading = false, needsVerification = true, error = null) }
                        resendCode()
                    } else {
                        val message = NetworkUtils.getErrorMessageFromException(context, error)
                        _uiState.update { it.copy(isLoading = false, error = message) }
                    }
                }
        }
    }

    private fun signUpWithEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = _uiState.value.email.trim().lowercase()
            authRepository.registerWithEmail(email, _uiState.value.password, _uiState.value.username)
                .onSuccess { response ->
                    val code = response.verificationCode
                    if (code != null) {
                        // Auto-verify with the code from response
                        _uiState.update { it.copy(needsVerification = true) }
                        verifyEmail(code)
                    } else {
                        _uiState.update { it.copy(isLoading = false, needsVerification = true) }
                        startResendTimer()
                    }
                }
                .onFailure { error -> 
                    val message = NetworkUtils.getErrorMessageFromException(context, error)
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    private fun verifyEmail(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = _uiState.value.email.trim().lowercase()
            authRepository.verifyEmail(email, code)
                .onSuccess { 
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, needsVerification = false) } 
                }
                .onFailure { error -> 
                    val message = NetworkUtils.getErrorMessageFromException(context, error)
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    private fun resendCode() {
        if (_uiState.value.resendTimer > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = _uiState.value.email.trim().lowercase()
            authRepository.resendCode(email)
                .onSuccess { data ->
                    val code = data["verificationCode"]
                    if (code != null) {
                        verifyEmail(code)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        startResendTimer()
                    }
                }
                .onFailure { error -> 
                    val message = NetworkUtils.getErrorMessageFromException(context, error)
                    _uiState.update { it.copy(isLoading = false, error = message) }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() } // Reset state
        }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val needsVerification: Boolean = false,
    val resendTimer: Int = 0,
    val error: String? = null
)

sealed class AuthEvent {
    data class GoogleSignIn(val context: Context) : AuthEvent()
    object EmailSignIn : AuthEvent()
    object EmailSignUp : AuthEvent()
    data class VerifyEmail(val code: String) : AuthEvent()
    object ToggleMode : AuthEvent()
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    data class UsernameChanged(val username: String) : AuthEvent()
    object DismissError : AuthEvent()
    object ResetVerificationState : AuthEvent()
    object Logout : AuthEvent()
    object ResendCode : AuthEvent()
}
