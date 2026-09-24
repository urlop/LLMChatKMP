package com.ruby.myllmchatkmp.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruby.myllmchatkmp.data.connectivity.ConnectivityObserver
import com.ruby.myllmchatkmp.presentation.chat.ChatIntent
import com.ruby.myllmchatkmp.presentation.chat.ChatScreen
import com.ruby.myllmchatkmp.presentation.chat.ChatViewModel
import com.ruby.myllmchatkmp.presentation.chat.ConversationListScreen
import com.ruby.myllmchatkmp.presentation.settings.SettingsScreen
import com.ruby.myllmchatkmp.presentation.settings.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private object Routes {
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = koinViewModel()
    val chatUiState by chatViewModel.uiState.collectAsState()
    val connectivityObserver: ConnectivityObserver = koinInject()
    val isOnline by connectivityObserver.observe().collectAsState(initial = true)

    NavHost(navController = navController, startDestination = Routes.CONVERSATIONS, modifier = Modifier) {
        composable(Routes.CONVERSATIONS) {
            ConversationListScreen(
                conversations = chatUiState.conversations,
                onSelect = { id ->
                    chatViewModel.onIntent(ChatIntent.SelectConversation(id))
                    navController.navigate(Routes.CHAT)
                },
                onNewConversation = {
                    chatViewModel.onIntent(ChatIntent.NewConversation)
                    navController.navigate(Routes.CHAT)
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                uiState = chatUiState,
                isOffline = !isOnline,
                onIntent = chatViewModel::onIntent,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsState()
            SettingsScreen(
                settings = settings,
                apiKeyDraft = settingsViewModel.apiKeyDraft,
                onApiKeyChange = settingsViewModel::onApiKeyChange,
                onModelChange = settingsViewModel::onModelChange,
                onTemperatureChange = settingsViewModel::onTemperatureChange,
                onThemeChange = settingsViewModel::onThemeChange,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
