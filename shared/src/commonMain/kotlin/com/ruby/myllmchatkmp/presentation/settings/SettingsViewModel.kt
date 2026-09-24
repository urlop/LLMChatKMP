package com.ruby.myllmchatkmp.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruby.myllmchatkmp.data.settings.AppSettings
import com.ruby.myllmchatkmp.data.settings.AppTheme
import com.ruby.myllmchatkmp.data.settings.SettingsRepository
import com.ruby.myllmchatkmp.data.storage.SecureStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val secureStorage: SecureStorage,
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        settingsRepository.observeSettings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    var apiKeyDraft by mutableStateOf(secureStorage.getApiKey().orEmpty())
        private set

    fun onApiKeyChange(value: String) {
        apiKeyDraft = value
        secureStorage.setApiKey(value.ifBlank { null })
    }

    fun onModelChange(model: String) {
        viewModelScope.launch { settingsRepository.updateModel(model) }
    }

    fun onTemperatureChange(temperature: Float) {
        viewModelScope.launch { settingsRepository.updateTemperature(temperature) }
    }

    fun onThemeChange(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.updateTheme(theme) }
    }
}
