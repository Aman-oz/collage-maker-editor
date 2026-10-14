package org.example.project.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the one [HttpClient] shared across the app (the central-data catalogs, the background
 * remover and the network image loader all use it). No engine is named on purpose: each platform source set puts exactly
 * one Ktor engine on the classpath (OkHttp on Android, Darwin on iOS), so Ktor auto-selects it —
 * that keeps the client configuration itself in `commonMain`.
 *
 * Unknown fields are ignored because the DTOs map only what the app reads: the central-data
 * responses carry many more keys (`views`, `tags`, `overrides`, …).
 */
internal fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            },
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 20_000
        socketTimeoutMillis = 20_000
    }
}
