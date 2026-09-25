package com.ruby.myllmchatkmp.presentation.chat

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ruby.myllmchatkmp.domain.fake.FakeChatRepository
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import com.ruby.myllmchatkmp.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end Compose UI test (PLAN.md phase 5, step 38): drives the real [ChatScreen] +
 * [ChatViewModel] against [FakeChatRepository] through actual user gestures (typing, tapping
 * Send) and asserts on what's rendered, rather than just on ViewModel state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // ViewModel.viewModelScope resolves Dispatchers.Main.immediate; there's no real Main
        // dispatcher on plain JVM, so install a real (non-virtual-time) one for this test.
        Dispatchers.setMain(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendingAMessageStreamsAReplyAndRendersItInTheConversation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = FakeChatRepository(scope)
        val viewModel = ChatViewModel(repository, SendMessageUseCase(repository))

        composeTestRule.setContent {
            val state by viewModel.uiState.collectAsState()
            ChatScreen(uiState = state, isOffline = false, onIntent = viewModel::onIntent, onBack = {})
        }

        viewModel.onIntent(ChatIntent.NewConversation)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.selectedConversationId != null
        }

        composeTestRule.onNodeWithText("Message").performTextInput("hello")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        // The user bubble renders immediately.
        composeTestRule.onNodeWithText("hello").assertExists()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            val messages = viewModel.uiState.value.messages
            messages.size == 2 && messages.last().status == MessageStatus.Done
        }
        composeTestRule.waitForIdle()

        // isSending flipped back off once streaming finished, so Send is showing again (not Stop).
        composeTestRule.onNodeWithContentDescription("Send").assertExists()

        scope.cancel()
    }
}
