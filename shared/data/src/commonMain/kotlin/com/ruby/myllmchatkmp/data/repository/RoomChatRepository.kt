package com.ruby.myllmchatkmp.data.repository

import com.ruby.myllmchatkmp.data.local.AppDatabase
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Conversation
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import com.ruby.myllmchatkmp.domain.repository.ChatRepository
import com.ruby.myllmchatkmp.domain.repository.ReplySource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * The DB is the single source of truth (roadmap phase 3): [replySource] events are persisted as
 * they arrive and [observeMessages] just reads the DB back, so it doesn't matter who's collecting
 * (or not collecting) [sendMessage]'s returned flow.
 */
class RoomChatRepository(
    database: AppDatabase,
    private val replySource: ReplySource,
    private val scope: CoroutineScope,
) : ChatRepository {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val streamingJobs = mutableMapOf<String, Job>()

    init {
        // A message stuck in Streaming/Sending only happens if the process died mid-reply.
        scope.launch { messageDao.markDanglingStreamsAsFailed() }
    }

    private fun newId(prefix: String): String = "$prefix-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(100_000)}"

    override fun observeConversations(): Flow<List<Conversation>> = conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    override suspend fun createConversation(title: String): Conversation {
        val now = Clock.System.now()
        val conversation = Conversation(id = newId("conv"), title = title, createdAt = now, updatedAt = now)
        conversationDao.upsert(conversation.toEntity())
        return conversation
    }

    override fun sendMessage(
        conversationId: String,
        prompt: String,
    ): Flow<ChatEvent> =
        callbackFlow {
            val now = Clock.System.now()
            val userMessage =
                Message(
                    id = newId("msg"),
                    conversationId = conversationId,
                    role = MessageRole.User,
                    content = prompt,
                    status = MessageStatus.Done,
                    createdAt = now,
                )
            messageDao.upsert(userMessage.toEntity())
            conversationDao.touch(conversationId, now.toEpochMilliseconds())

            val assistantId = newId("msg")
            messageDao.upsert(
                Message(
                    id = assistantId,
                    conversationId = conversationId,
                    role = MessageRole.Assistant,
                    content = "",
                    status = MessageStatus.Sending,
                    createdAt = Clock.System.now(),
                ).toEntity(),
            )

            val job =
                scope.launch {
                    streamAndPersist(conversationId, assistantId, sink = ::trySend)
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
            val existing = messageDao.getById(messageId)
            if (existing != null) {
                messageDao.upsert(existing.copy(content = "", status = MessageStatus.Sending.name))
            }
            val job =
                scope.launch {
                    streamAndPersist(conversationId, messageId, sink = ::trySend)
                    close()
                }
            streamingJobs[conversationId] = job
            awaitClose { job.cancel() }
        }

    override suspend fun stopStreaming(conversationId: String) {
        // Wait for the cancellation to actually land (including streamAndPersist's own
        // catch-block write) before repairing status below, or a write from the cancelled
        // job can land after this one and clobber it back to Streaming/Failed.
        streamingJobs.remove(conversationId)?.cancelAndJoin()
        val messages = messageDao.observeForConversation(conversationId).first()
        val unfinished = messages.lastOrNull { it.status == MessageStatus.Streaming.name || it.status == MessageStatus.Sending.name }
        if (unfinished != null) {
            messageDao.upsert(unfinished.copy(status = MessageStatus.Done.name))
        }
    }

    private suspend fun streamAndPersist(
        conversationId: String,
        assistantId: String,
        sink: (ChatEvent) -> Unit,
    ) {
        val history =
            messageDao
                .observeForConversation(conversationId)
                .first()
                .map { it.toDomain() }
                .filter { it.status == MessageStatus.Done }

        val existing = messageDao.getById(assistantId) ?: return
        var entity = existing.copy(status = MessageStatus.Streaming.name)
        messageDao.upsert(entity)

        val builder = StringBuilder(entity.content)
        var lastFlush = TimeSource.Monotonic.markNow()

        try {
            replySource.streamReply(conversationId, history).collect { event ->
                when (event) {
                    is ChatEvent.Delta -> {
                        builder.append(event.text)
                        sink(event)
                        if (lastFlush.elapsedNow() >= 50.milliseconds) {
                            entity = entity.copy(content = builder.toString())
                            messageDao.upsert(entity)
                            lastFlush = TimeSource.Monotonic.markNow()
                        }
                    }
                    ChatEvent.Done -> {
                        entity = entity.copy(content = builder.toString(), status = MessageStatus.Done.name)
                        messageDao.upsert(entity)
                        sink(event)
                    }
                    is ChatEvent.Error -> {
                        entity = entity.copy(content = builder.toString(), status = MessageStatus.Failed.name)
                        messageDao.upsert(entity)
                        sink(event)
                    }
                }
            }
        } catch (e: CancellationException) {
            entity = entity.copy(content = builder.toString(), status = MessageStatus.Failed.name)
            messageDao.upsert(entity)
            throw e
        }
    }
}
