package com.chatora.shared.ui

import androidx.compose.runtime.*
import com.chatora.shared.repository.AuthState
import com.chatora.shared.ui.screens.AuthScreen
import com.chatora.shared.ui.screens.HomeScreen
import com.chatora.shared.ui.theme.ChatoraTheme
import com.chatora.shared.viewmodel.AuthViewModel
import com.chatora.shared.viewmodel.MatchViewModel

/**
 * Root composable for the Chatora app — shared across Web and iOS.
 * Manages authentication-based navigation between AuthScreen and HomeScreen.
 */
@Composable
fun App(
    authViewModel: AuthViewModel,
    matchViewModel: MatchViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    ChatoraTheme(darkTheme = true) {
        when (val state = authState) {
            is AuthState.LoggedIn -> {
                HomeScreen(
                    username = state.username,
                    matchViewModel = matchViewModel,
                    onLogout = { authViewModel.logout() }
                )
            }
            else -> {
                AuthScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { /* Navigation handled by state */ }
                )
            }
        }
    }
}
