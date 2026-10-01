package com.ruby.myllmchatkmp.data.repository

import com.ruby.myllmchatkmp.data.network.ChatApiConfig
import com.ruby.myllmchatkmp.data.network.ChatCompletionsApi
import com.ruby.myllmchatkmp.data.network.createHttpClient
import com.ruby.myllmchatkmp.data.network.ChatMessageDto
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.repository.ReplySource
import kotlinx.coroutines.flow.Flow

/**
 * Real [ReplySource] backed by [ChatCompletionsApi]. NEEDS A HUMAN DECISION: not wired into the
 * default Koin graph -- see PLAN.md phase 3 note for what's needed (provider choice + API key)
 * before this can be the active binding.
 */
class RemoteReplySource(
    private val api: ChatCompletionsApi,
) : ReplySource {
    override fun streamReply(
        conversationId: String,
        history: List<Message>,
    ): Flow<ChatEvent> = api.streamReply(history.map { it.toDto() })

    private fun Message.toDto() =
        ChatMessageDto(
            role =
                when (role) {
                    MessageRole.User -> "user"
                    MessageRole.Assistant -> "assistant"
                    MessageRole.System -> "system"
                },
            content = content,
        )
}

/** Builds a [RemoteReplySource] with its own HTTP client; [configProvider] is resolved on every request. */
fun createRemoteReplySource(configProvider: suspend () -> ChatApiConfig): RemoteReplySource =
    RemoteReplySource(ChatCompletionsApi(createHttpClient(), configProvider))
