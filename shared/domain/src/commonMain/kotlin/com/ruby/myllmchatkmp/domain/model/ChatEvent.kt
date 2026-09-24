package com.ruby.myllmchatkmp.domain.model

sealed interface ChatEvent {
    data class Delta(
        val text: String,
    ) : ChatEvent

    data object Done : ChatEvent

    data class Error(
        val error: ChatError,
    ) : ChatEvent
}
