package com.ruby.myllmchatkmp.presentation.chat

import com.ruby.myllmchatkmp.domain.model.ChatError
import com.ruby.myllmchatkmp.domain.model.Conversation
import com.ruby.myllmchatkmp.domain.model.Message

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val selectedConversationId: String? = null,
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val error: ChatError? = null,
) {
    val isEmpty: Boolean get() = selectedConversationId != null && messages.isEmpty()
}

fun ChatError.toDisplayMessage(): String =
    when (this) {
        ChatError.NoNetwork -> "No network connection."
        ChatError.Unauthorized -> "Invalid API key."
        ChatError.RateLimited -> "Rate limited, try again shortly."
        ChatError.Timeout -> "Request timed out."
        is ChatError.Unknown -> message
    }

sealed interface ChatIntent {
    data class UpdateDraft(
        val text: String,
    ) : ChatIntent

    data class Send(
        val text: String,
    ) : ChatIntent

    data object Stop : ChatIntent

    data class Retry(
        val messageId: String,
    ) : ChatIntent

    data object NewConversation : ChatIntent

    data class SelectConversation(
        val conversationId: String,
    ) : ChatIntent

    data object DismissError : ChatIntent
}
