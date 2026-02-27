package com.chatora.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.chatora.shared.di.sharedModule
import com.chatora.shared.ui.App
import com.chatora.shared.viewmodel.AuthViewModel
import com.chatora.shared.viewmodel.MatchViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin DI (only once)
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(sharedModule)
        }
    }

    val koin = GlobalContext.get()
    val authViewModel: AuthViewModel = koin.get()
    val matchViewModel: MatchViewModel = koin.get()

    CanvasBasedWindow(title = "Chatora") {
        App(
            authViewModel = authViewModel,
            matchViewModel = matchViewModel
        )
    }
}
