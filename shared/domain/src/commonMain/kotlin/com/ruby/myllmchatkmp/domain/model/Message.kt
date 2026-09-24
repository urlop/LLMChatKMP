package com.ruby.myllmchatkmp.domain.model

import kotlin.time.Instant

enum class MessageRole {
    User,
    Assistant,
    System,
}

enum class MessageStatus {
    Sending,
    Streaming,
    Done,
    Failed,
}

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val status: MessageStatus,
    val createdAt: Instant,
)
