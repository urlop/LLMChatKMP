package com.ruby.myllmchatkmp.domain.model

sealed class ChatError {
    data object NoNetwork : ChatError()

    data object Unauthorized : ChatError()

    data object RateLimited : ChatError()

    data object Timeout : ChatError()

    data class Unknown(
        val message: String,
    ) : ChatError()
}
