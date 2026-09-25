package com.ruby.myllmchatkmp.data.repository

import androidx.room.Room
import com.ruby.myllmchatkmp.data.local.AppDatabase
import com.ruby.myllmchatkmp.data.local.buildDatabase
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Exercises [RoomChatRepository] against a real (file-backed) Room database and the
 * [FakeReplySource], rather than fakes on both sides -- the DB-as-source-of-truth design
 * (roadmap phase 3) is only actually verified once persistence and streaming run together.
 */
class RoomChatRepositoryTest {
    private val dbFile = File.createTempFile("room-chat-repository-test", ".db")

    private fun openDatabase(): AppDatabase =
        buildDatabase(
            Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath),
        )

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    @Test
    fun sendMessagePersistsUserAndAssistantMessagesAsTheyStream() =
        runTest {
            val database = openDatabase()
            val repository = RoomChatRepository(database, FakeReplySource(), backgroundScope)
            val conversation = repository.createConversation("test chat")

            val events = repository.sendMessage(conversation.id, "hello").toList()
            assertEquals(ChatEvent.Done, events.last())

            val messages = repository.observeMessages(conversation.id).first()
            assertEquals(2, messages.size)
            assertEquals(MessageRole.User, messages[0].role)
            assertEquals("hello", messages[0].content)
            assertEquals(MessageStatus.Done, messages[0].status)
            assertEquals(MessageRole.Assistant, messages[1].role)
            assertEquals(MessageStatus.Done, messages[1].status)
            assertTrue(messages[1].content.isNotBlank())

            // The persisted content must match what the stream actually emitted, not just be non-blank.
            val streamedText =
                events
                    .filterIsInstance<ChatEvent.Delta>()
                    .joinToString(separator = "") { it.text }
            assertEquals(streamedText, messages[1].content)

            database.close()
        }

    @Test
    fun retryMessageReStreamsAFailedAssistantReplyBackToDone() =
        runTest {
            val database = openDatabase()
            val repository = RoomChatRepository(database, FakeReplySource(), backgroundScope)
            val conversation = repository.createConversation("test chat")
            repository.sendMessage(conversation.id, "hello").toList()

            val before = repository.observeMessages(conversation.id).first()
            val assistantMessage = before.last { it.role == MessageRole.Assistant }
            database.messageDao().upsert(
                database.messageDao().getById(assistantMessage.id)!!.copy(status = MessageStatus.Failed.name),
            )

            val retryEvents = repository.retryMessage(conversation.id, assistantMessage.id).toList()
            assertEquals(ChatEvent.Done, retryEvents.last())

            val after = repository.observeMessages(conversation.id).first()
            val retried = after.first { it.id == assistantMessage.id }
            assertEquals(MessageStatus.Done, retried.status)
            assertTrue(retried.content.isNotBlank())

            database.close()
        }

    // Plain runBlocking (real dispatchers, real time) rather than runTest: Room's DB coroutines
    // run on the real Dispatchers.Default (see AppDatabase.buildDatabase), so pinning "mid-stream"
    // with virtual time would race against that real dispatcher instead of deterministically
    // landing before the first delta.
    @Test
    fun stopStreamingCancelsTheInFlightReplyAndLeavesNoDanglingStatus() =
        runBlocking {
            val database = openDatabase()
            val repository = RoomChatRepository(database, FakeReplySource(), this)
            val conversation = repository.createConversation("test chat")

            val collected = mutableListOf<ChatEvent>()
            val collectJob =
                launch {
                    repository.sendMessage(conversation.id, "hello").collect { collected += it }
                }
            while (collected.isEmpty()) {
                yield()
            }

            repository.stopStreaming(conversation.id)
            collectJob.cancelAndJoin()

            val messages = repository.observeMessages(conversation.id).first()
            val assistantMessage = messages.first { it.role == MessageRole.Assistant }
            assertNotEquals(MessageStatus.Streaming, assistantMessage.status)
            assertNotEquals(MessageStatus.Sending, assistantMessage.status)

            database.close()
        }
}
