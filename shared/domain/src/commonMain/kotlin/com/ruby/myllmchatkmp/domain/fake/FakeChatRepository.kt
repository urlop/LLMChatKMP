package com.ruby.myllmchatkmp.domain.fake

import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Conversation
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import com.ruby.myllmchatkmp.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

/**
 * Streams canned text instead of calling a real LLM, so UI work never needs an API key.
 * Kept as a Koin binding for demo/offline mode even once a real backend exists (see
 * PLAN.md phase 6, "fake-backend mode").
 */
class FakeChatRepository(
    private val scope: CoroutineScope,
) : ChatRepository {
    private val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val messagesByConversation = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    private val streamingJobs = mutableMapOf<String, Job>()
    private var nextId = 0

    private fun newId(prefix: String): String = "$prefix-${nextId++}"

    override fun observeConversations(): Flow<List<Conversation>> = conversations.asStateFlow()

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messagesByConversation.asStateFlow().map { it[conversationId].orEmpty() }

    override suspend fun createConversation(title: String): Conversation {
        val now = Clock.System.now()
        val conversation = Conversation(id = newId("conv"), title = title, createdAt = now, updatedAt = now)
        conversations.update { it + conversation }
        messagesByConversation.update { it + (conversation.id to emptyList()) }
        return conversation
    }

    override fun sendMessage(
        conversationId: String,
        prompt: String,
    ): Flow<ChatEvent> =
        callbackFlow {
            val userMessage =
                Message(
                    id = newId("msg"),
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = prompt,
                    status = MessageStatus.Done,
                    createdAt = Clock.System.now(),
                )
            appendMessage(userMessage)

            val assistantId = newId("msg")
            appendMessage(
                Message(
                    id = assistantId,
                    conversationId = conversationId,
                    role = MessageRole.Assistant,
                    content = "",
                    status = MessageStatus.Sending,
                    createdAt = Clock.System.now(),
                ),
            )

            val job =
                scope.launch {
                    streamCannedReply(conversationId, assistantId, promptSeed = prompt, sink = ::trySend)
                    close()
                }
            streamingJobs[conversationId] = job
            awaitClose { job.cancel() }
        }

    override fun retryMessage(
        conversationId: String,
        messageId: String,
    ): Flow<ChatEvent> =
        callbackFlow {
            updateMessage(conversationId, messageId) { it.copy(content = "", status = MessageStatus.Sending) }
            val job =
                scope.launch {
                    streamCannedReply(conversationId, messageId, promptSeed = messageId, sink = ::trySend)
                    close()
                }
            streamingJobs[conversationId] = job
            awaitClose { job.cancel() }
        }

    override suspend fun stopStreaming(conversationId: String) {
        streamingJobs.remove(conversationId)?.cancel()
        val messages = messagesByConversation.value[conversationId].orEmpty()
        val streaming = messages.lastOrNull { it.status == MessageStatus.Streaming || it.status == MessageStatus.Sending }
        if (streaming != null) {
            updateMessage(conversationId, streaming.id) { it.copy(status = MessageStatus.Done) }
        }
    }

    private suspend fun streamCannedReply(
        conversationId: String,
        assistantId: String,
        promptSeed: String,
        sink: (ChatEvent) -> Unit,
    ) {
        val reply = cannedReply(promptSeed)
        val words = reply.split(" ")
        updateMessage(conversationId, assistantId) { it.copy(status = MessageStatus.Streaming) }
        val builder = StringBuilder()
        for (word in words) {
            kotlinx.coroutines.delay(40)
            builder.append(if (builder.isEmpty()) word else " $word")
            val delta = if (builder.isEmpty()) word else " $word"
            updateMessage(conversationId, assistantId) { it.copy(content = builder.toString()) }
            sink(ChatEvent.Delta(delta))
        }
        updateMessage(conversationId, assistantId) { it.copy(status = MessageStatus.Done) }
        sink(ChatEvent.Done)
    }

    private fun cannedReply(seed: String): String {
        val samples =
            listOf(
                "This is a canned reply from the fake chat repository, streamed word by word so the UI can " +
                    "exercise the same streaming, cancel, and retry paths a real backend would use.",
                "Here's a demo response. Wire up a real ChatRepository implementation and API key when you're " +
                    "ready to talk to an actual model.",
                "Fake backend online. Everything you see here -- streaming cursor, auto-scroll, retry -- works " +
                    "the same once a real network-backed repository is swapped in.",
            )
        val index = Random(seed.hashCode()).nextInt(samples.size)
        return samples[index]
    }

    private fun appendMessage(message: Message) {
        messagesByConversation.update { map ->
            val existing = map[message.conversationId].orEmpty()
            map + (message.conversationId to (existing + message))
        }
    }

    private fun updateMessage(
        conversationId: String,
        messageId: String,
        transform: (Message) -> Message,
    ) {
        messagesByConversation.update { map ->
            val existing = map[conversationId].orEmpty()
            val updated = existing.map { if (it.id == messageId) transform(it) else it }
            map + (conversationId to updated)
        }
    }
}
