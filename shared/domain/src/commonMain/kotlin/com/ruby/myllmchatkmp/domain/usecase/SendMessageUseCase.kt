package com.ruby.myllmchatkmp.domain.usecase

import com.ruby.myllmchatkmp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.collect

/**
 * Drives the send flow: the repository saves the user message and persists streamed deltas
 * as they arrive, so the UI never reads this use case's return value -- it observes the
 * repository's message flow instead. This just needs to be collected to happen.
 */
class SendMessageUseCase(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        prompt: String,
    ) {
        repository.sendMessage(conversationId, prompt).collect { }
    }
}
