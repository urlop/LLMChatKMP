package com.ruby.myllmchatkmp.di

import com.ruby.myllmchatkmp.data.local.buildDatabase
import com.ruby.myllmchatkmp.data.repository.FakeReplySource
import com.ruby.myllmchatkmp.data.repository.RoomChatRepository
import com.ruby.myllmchatkmp.data.settings.DataStoreSettingsRepository
import com.ruby.myllmchatkmp.data.settings.SettingsRepository
import com.ruby.myllmchatkmp.domain.repository.ChatRepository
import com.ruby.myllmchatkmp.domain.repository.ReplySource
import com.ruby.myllmchatkmp.domain.usecase.SendMessageUseCase
import com.ruby.myllmchatkmp.presentation.chat.ChatViewModel
import com.ruby.myllmchatkmp.presentation.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Platform-specific pieces (DB path/context, DataStore file, secure storage) come from [platformModule]. */
fun commonModule() =
    module {
        single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
        single { buildDatabase(get()) }
        // Default binding: canned replies, so the app runs with no API key -- see PLAN.md phase 3 note
        // for what's needed to swap in a RemoteReplySource instead.
        single<ReplySource> { FakeReplySource() }
        single<ChatRepository> { RoomChatRepository(get(), get(), get()) }
        single { SendMessageUseCase(get()) }
        single<SettingsRepository> { DataStoreSettingsRepository(get()) }
        viewModel { ChatViewModel(get(), get()) }
        viewModel { SettingsViewModel(get(), get()) }
    }

expect fun platformModule(): Module
