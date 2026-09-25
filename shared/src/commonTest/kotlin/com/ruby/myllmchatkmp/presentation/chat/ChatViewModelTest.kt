package com.ruby.myllmchatkmp.presentation.chat

import app.cash.turbine.test
import com.ruby.myllmchatkmp.domain.fake.FakeChatRepository
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import com.ruby.myllmchatkmp.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(scope: kotlinx.coroutines.CoroutineScope): ChatViewModel {
        val repository = FakeChatRepository(scope)
        return ChatViewModel(repository, SendMessageUseCase(repository))
    }

    @Test
    fun sendingAMessageStreamsAnAssistantReplyToDone() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(this)

            viewModel.uiState.test {
                var state = awaitItem()
                viewModel.onIntent(ChatIntent.NewConversation)
                while (state.selectedConversationId == null) {
                    state = awaitItem()
                }

                viewModel.onIntent(ChatIntent.Send("hello"))

                // Draft clears and isSending flips on -- other collectors (e.g. the messages flow
                // that selecting a conversation just started) may interleave their own emissions,
                // so poll for the condition rather than assuming it's the very next item.
                while (!(state.draft.isEmpty() && state.isSending)) {
                    state = awaitItem()
                }

                // Messages arrive as the fake streams; wait for the final Done state.
                var lastMessages = state.messages
                while (lastMessages.size < 2 || lastMessages.last().status != MessageStatus.Done) {
                    state = awaitItem()
                    lastMessages = state.messages
                }

                assertEquals(2, lastMessages.size)
                assertEquals(MessageRole.User, lastMessages[0].role)
                assertEquals("hello", lastMessages[0].content)
                assertEquals(MessageRole.Assistant, lastMessages[1].role)
                assertTrue(lastMessages[1].content.isNotBlank())

                // isSending eventually flips back off.
                while (state.isSending) {
                    state = awaitItem()
                }
                assertTrue(!state.isSending)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun updateDraftIntentUpdatesStateImmediately() =
        runTest(dispatcher) {
            val viewModel = buildViewModel(this)

            viewModel.uiState.test {
                awaitItem()
                viewModel.onIntent(ChatIntent.UpdateDraft("wip"))
                val state = awaitItem()
                assertEquals("wip", state.draft)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dismissErrorClearsTheErrorState() {
        val viewModel = buildViewModel(kotlinx.coroutines.CoroutineScope(dispatcher))
        assertNotNull(viewModel.uiState.value)

        viewModel.onIntent(ChatIntent.DismissError)
        assertEquals(null, viewModel.uiState.value.error)
    }
}
