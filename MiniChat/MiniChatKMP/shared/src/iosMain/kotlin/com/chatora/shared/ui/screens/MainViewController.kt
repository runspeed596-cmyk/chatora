package com.chatora.shared.ui.screens

import androidx.compose.runtime.Composable

/**
 * iOS-specific main entry point.
 * Called from Swift's ContentView to embed the shared Compose UI.
 */
@Composable
fun MainViewController() = App()

@Composable
private fun App() {
    // iOS apps should initialize Koin in the Swift layer
    // and pass ViewModels to the shared App composable.
    // This is a convenience function for simple setups.
    com.chatora.shared.ui.App(
        authViewModel = com.chatora.shared.viewmodel.AuthViewModel(
            com.chatora.shared.repository.AuthRepository(
                com.chatora.shared.network.ChatoraApi(
                    com.chatora.shared.network.createHttpClient(),
                    com.chatora.shared.di.DEFAULT_API_BASE_URL
                )
            )
        ),
        matchViewModel = com.chatora.shared.viewmodel.MatchViewModel()
    )
}
