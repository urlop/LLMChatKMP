package com.ruby.myllmchatkmp.domain.fake

import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FakeChatRepositoryTest {
    @Test
    fun sendMessagePersistsUserMessageThenStreamsAssistantReplyToDone() =
        runTest {
            val repository = FakeChatRepository(scope = this)
            val conversation = repository.createConversation("Test")

            repository.sendMessage(conversation.id, "hello").collect { }

            val messages = repository.observeMessages(conversation.id).first()
            assertEquals(2, messages.size)
            assertEquals(MessageRole.User, messages[0].role)
            assertEquals("hello", messages[0].content)
            assertEquals(MessageRole.Assistant, messages[1].role)
            assertEquals(MessageStatus.Done, messages[1].status)
            assertTrue(messages[1].content.isNotBlank())
        }

    @Test
    fun stopStreamingFinalizesThePartialAssistantMessage() =
        runTest {
            val repository = FakeChatRepository(scope = this)
            val conversation = repository.createConversation("Test")

            val job =
                launch {
                    repository.sendMessage(conversation.id, "hello").collect { }
                }
            runCurrent()
            repository.stopStreaming(conversation.id)
            job.cancel()

            val messages = repository.observeMessages(conversation.id).first()
            val assistantMessage = messages.last()
            assertEquals(MessageStatus.Done, assistantMessage.status)
        }
}
