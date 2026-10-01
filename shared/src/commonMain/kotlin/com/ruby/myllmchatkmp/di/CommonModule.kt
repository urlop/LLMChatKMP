package com.ruby.myllmchatkmp.di

import com.ruby.myllmchatkmp.data.local.buildDatabase
import com.ruby.myllmchatkmp.data.network.ChatApiConfig
import com.ruby.myllmchatkmp.data.repository.ConfiguredReplySource
import com.ruby.myllmchatkmp.data.repository.FakeReplySource
import com.ruby.myllmchatkmp.data.repository.createRemoteReplySource
import com.ruby.myllmchatkmp.data.repository.RoomChatRepository
import com.ruby.myllmchatkmp.data.settings.DataStoreSettingsRepository
import com.ruby.myllmchatkmp.data.settings.SettingsRepository
import com.ruby.myllmchatkmp.data.storage.SecureStorage
import com.ruby.myllmchatkmp.domain.repository.ChatRepository
import com.ruby.myllmchatkmp.domain.repository.ReplySource
import com.ruby.myllmchatkmp.domain.usecase.SendMessageUseCase
import com.ruby.myllmchatkmp.presentation.chat.ChatViewModel
import com.ruby.myllmchatkmp.presentation.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val GROQ_BASE_URL = "https://api.groq.com/openai/v1"

/** Platform-specific pieces (DB path/context, DataStore file, secure storage) come from [platformModule]. */
fun commonModule() =
    module {
        single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single { buildDatabase(get()) }
        // Real Groq replies once a key is saved in Settings; canned replies otherwise.
        single<ReplySource> {
            val storage = get<SecureStorage>()
            val settings = get<SettingsRepository>()
            val remote =
                createRemoteReplySource {
                    // Resolved per request so Settings edits apply immediately.
                    val current = settings.observeSettings().first()
                    ChatApiConfig(
                        baseUrl = GROQ_BASE_URL,
                        apiKey = storage.getApiKey().orEmpty(),
                        model = current.model,
                        temperature = current.temperature,
                    )
                }
            ConfiguredReplySource(storage, remote, FakeReplySource())
        }
        single<ChatRepository> { RoomChatRepository(get(), get(), get()) }
        single { SendMessageUseCase(get()) }
        single<SettingsRepository> { DataStoreSettingsRepository(get()) }
        viewModel { ChatViewModel(get(), get()) }
        viewModel { SettingsViewModel(get(), get()) }
    }

expect fun platformModule(): Module
