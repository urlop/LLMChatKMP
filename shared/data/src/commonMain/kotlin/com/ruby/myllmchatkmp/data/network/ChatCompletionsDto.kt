package com.ruby.myllmchatkmp.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTOs for an OpenAI-compatible `/chat/completions` endpoint (see [ChatApiConfig] for why). */
@Serializable
data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionChunkDto(
    val choices: List<ChoiceDto> = emptyList(),
)

@Serializable
data class ChoiceDto(
    val delta: DeltaDto = DeltaDto(),
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class DeltaDto(
    val content: String? = null,
)
