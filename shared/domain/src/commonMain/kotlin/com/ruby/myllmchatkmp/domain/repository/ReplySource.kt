package com.ruby.myllmchatkmp.domain.repository

import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Produces the assistant's reply for a conversation. Lets [ChatRepository] implementations
 * (persistence, retry/stop, DB-as-source-of-truth) stay the same whether the reply comes from
 * canned text (no API key needed) or a real LLM endpoint.
 */
fun interface ReplySource {
    fun streamReply(
        conversationId: String,
        history: List<Message>,
    ): Flow<ChatEvent>
}
