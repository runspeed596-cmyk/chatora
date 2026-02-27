package com.chatora.shared.di

import com.chatora.shared.network.ChatoraApi
import com.chatora.shared.network.createHttpClient
import com.chatora.shared.repository.AuthRepository
import com.chatora.shared.viewmodel.AuthViewModel
import com.chatora.shared.viewmodel.MatchViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Base URL for the Chatora API.
 * Override via Koin when initializing the app on each platform.
 */
const val DEFAULT_API_BASE_URL = "http://192.168.70.113:8080"

/**
 * Shared Koin modules for DI across all platforms.
 */
val sharedModule = module {
    // HTTP Client
    single { createHttpClient() }

    // API
    single { ChatoraApi(get(), DEFAULT_API_BASE_URL) }

    // Repositories
    singleOf(::AuthRepository)

    // ViewModels
    factory { AuthViewModel(get()) }
    factory { MatchViewModel() }
}
