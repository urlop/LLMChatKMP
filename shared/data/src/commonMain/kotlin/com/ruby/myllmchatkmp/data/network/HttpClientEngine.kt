package com.ruby.myllmchatkmp.data.network

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android/JVM, Darwin (NSURLSession) on iOS. */
expect fun createPlatformEngine(): HttpClientEngine
