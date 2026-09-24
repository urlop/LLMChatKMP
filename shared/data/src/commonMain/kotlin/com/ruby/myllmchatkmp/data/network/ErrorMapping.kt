package com.ruby.myllmchatkmp.data.network

import com.ruby.myllmchatkmp.domain.model.ChatError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException

fun mapHttpStatusToChatError(status: HttpStatusCode): ChatError =
    when (status) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ChatError.Unauthorized
        HttpStatusCode.TooManyRequests -> ChatError.RateLimited
        HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> ChatError.Timeout
        else -> ChatError.Unknown("HTTP ${status.value}")
    }

fun mapThrowableToChatError(throwable: Throwable): ChatError =
    when (throwable) {
        is CancellationException -> throw throwable
        is HttpRequestTimeoutException -> ChatError.Timeout
        is IOException -> ChatError.NoNetwork
        else -> ChatError.Unknown(throwable.message ?: "Unknown error")
    }
