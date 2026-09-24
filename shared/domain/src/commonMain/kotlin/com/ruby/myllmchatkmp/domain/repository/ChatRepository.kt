package com.ruby.myllmchatkmp.domain.repository

import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Conversation
import com.ruby.myllmchatkmp.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for conversations and messages. Implementations persist state
 * (DB in production, in-memory for [com.ruby.myllmchatkmp.domain.fake.FakeChatRepository]);
 * callers only ever read through [observeConversations]/[observeMessages].
 */
interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>

    fun observeMessages(conversationId: String): Flow<List<Message>>

    suspend fun createConversation(title: String = "New chat"): Conversation

    /** Saves the user message, then streams the assistant reply, persisting deltas as they arrive. */
    fun sendMessage(
        conversationId: String,
        prompt: String,
    ): Flow<ChatEvent>

    /** Retries a message stuck in [com.ruby.myllmchatkmp.domain.model.MessageStatus.Failed]. */
    fun retryMessage(
        conversationId: String,
        messageId: String,
    ): Flow<ChatEvent>

    suspend fun stopStreaming(conversationId: String)
}
