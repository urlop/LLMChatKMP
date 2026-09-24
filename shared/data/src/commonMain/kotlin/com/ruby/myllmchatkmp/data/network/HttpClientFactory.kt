package com.ruby.myllmchatkmp.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(
    engine: HttpClientEngine = createPlatformEngine(),
    json: Json = defaultJson,
): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.INFO }
    }

val defaultJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
