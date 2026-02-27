package com.chatora.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Creates a pre-configured Ktor HttpClient.
 * The engine is provided by each platform (JS / Darwin).
 */
fun createHttpClient(): HttpClient {
    return HttpClient {
        // JSON serialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
                encodeDefaults = true
            })
        }

        // Logging
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        // WebSocket support
        install(WebSockets)

        // Timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }

        // Default request config
        defaultRequest {
            headers.append("Accept", "application/json")
        }
    }
}
