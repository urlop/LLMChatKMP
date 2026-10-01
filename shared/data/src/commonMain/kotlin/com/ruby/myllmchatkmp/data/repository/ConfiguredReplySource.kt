package com.ruby.myllmchatkmp.data.repository

import com.ruby.myllmchatkmp.data.storage.SecureStorage
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.repository.ReplySource
import kotlinx.coroutines.flow.Flow

/** Uses [remote] once an API key has been saved in Settings, otherwise [fallback] (canned replies). */
class ConfiguredReplySource(
    private val secureStorage: SecureStorage,
    private val remote: ReplySource,
    private val fallback: ReplySource,
) : ReplySource {
    override fun streamReply(
        conversationId: String,
        history: List<Message>,
    ): Flow<ChatEvent> =
        if (secureStorage.getApiKey().isNullOrBlank()) {
            fallback.streamReply(conversationId, history)
        } else {
            remote.streamReply(conversationId, history)
        }
}
