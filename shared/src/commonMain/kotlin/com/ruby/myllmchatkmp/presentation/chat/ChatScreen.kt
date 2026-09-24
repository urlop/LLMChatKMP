package com.ruby.myllmchatkmp.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    isOffline: Boolean,
    onIntent: (ChatIntent) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            lastVisible >= uiState.messages.lastIndex - 1
        }
    }

    // Auto-scroll to new content, but stop if the user has scrolled up to read history.
    LaunchedEffect(uiState.messages.size) {
        if (isAtBottom && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", modifier = Modifier.semantics { contentDescription = "Back" })
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isOffline) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "You're offline. Replies will resume once you're back online.",
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            uiState.error?.let { error ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(error.toDisplayMessage(), color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = { onIntent(ChatIntent.DismissError) }) { Text("Dismiss") }
                    }
                }
            }

            if (uiState.selectedConversationId == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isEmpty) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Say hello to start the conversation.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message = message, onRetry = { onIntent(ChatIntent.Retry(message.id)) })
                    }
                }
            }

            InputBar(
                draft = uiState.draft,
                isSending = uiState.isSending,
                onDraftChange = { onIntent(ChatIntent.UpdateDraft(it)) },
                onSend = { onIntent(ChatIntent.Send(uiState.draft)) },
                onStop = { onIntent(ChatIntent.Stop) },
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onRetry: () -> Unit,
) {
    val isUser = message.role == MessageRole.User
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isUser) {
                        Text(message.content)
                    } else {
                        Markdown(content = message.content.ifBlank { if (message.status == MessageStatus.Sending) "…" else "" })
                    }
                    if (message.status == MessageStatus.Streaming) {
                        Text("▍", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (message.status == MessageStatus.Failed) {
                        Text("Failed to reply.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (message.status == MessageStatus.Failed) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun InputBar(
    draft: String,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message") },
        )
        if (isSending) {
            IconButton(onClick = onStop) {
                Text("■", modifier = Modifier.semantics { contentDescription = "Stop" })
            }
        } else {
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Text("➤", modifier = Modifier.semantics { contentDescription = "Send" })
            }
        }
    }
}
