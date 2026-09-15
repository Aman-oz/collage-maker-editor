package org.example.project.data.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the one [HttpClient] shared across the app (the collage-layout repository and the network
 * image loader both use it). No engine is named on purpose: each platform source set puts exactly
 * one Ktor engine on the classpath (OkHttp on Android, Darwin on iOS), so Ktor auto-selects it —
 * that keeps the client configuration itself in `commonMain`.
 *
 * The JSON config mirrors the server's real quirks (see the collage layout endpoints): unknown
 * fields must be ignored (responses carry extra keys like `categoryName`), and lenient parsing lets
 * the numeric slot coordinates arrive as quoted strings (`"669"`) — see [org.example.project.data.collage.LenientFloatSerializer].
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
