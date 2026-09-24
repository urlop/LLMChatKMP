package com.ruby.myllmchatkmp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.repository.ChatRepository
import com.ruby.myllmchatkmp.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            chatRepository.observeConversations().collect { conversations ->
                val alreadySelected = _uiState.value.selectedConversationId
                _uiState.update { it.copy(conversations = conversations) }
                if (alreadySelected == null && conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
                }
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateDraft -> _uiState.update { it.copy(draft = intent.text) }
            is ChatIntent.Send -> send(intent.text)
            ChatIntent.Stop -> stop()
            is ChatIntent.Retry -> retry(intent.messageId)
            ChatIntent.NewConversation -> viewModelScope.launch { newConversation() }
            is ChatIntent.SelectConversation -> selectConversation(intent.conversationId)
            ChatIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun send(text: String) {
        val conversationId = _uiState.value.selectedConversationId ?: return
        if (text.isBlank()) return
        _uiState.update { it.copy(draft = "", isSending = true) }
        viewModelScope.launch {
            try {
                sendMessageUseCase(conversationId, text)
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    private fun stop() {
        val conversationId = _uiState.value.selectedConversationId ?: return
        viewModelScope.launch { chatRepository.stopStreaming(conversationId) }
    }

    private fun retry(messageId: String) {
        val conversationId = _uiState.value.selectedConversationId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                chatRepository.retryMessage(conversationId, messageId).collect { event ->
                    if (event is ChatEvent.Error) {
                        _uiState.update { it.copy(error = event.error) }
                    }
                }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    private suspend fun newConversation() {
        val conversation = chatRepository.createConversation()
        selectConversation(conversation.id)
    }

    private fun selectConversation(conversationId: String) {
        _uiState.update { it.copy(selectedConversationId = conversationId) }
        messagesJob?.cancel()
        messagesJob =
            viewModelScope.launch {
                chatRepository.observeMessages(conversationId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            }
    }
}
