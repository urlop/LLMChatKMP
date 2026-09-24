package com.ruby.myllmchatkmp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun updateModel(model: String)

    suspend fun updateTemperature(temperature: Float)

    suspend fun updateTheme(theme: AppTheme)
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map { prefs ->
            AppSettings(
                model = prefs[MODEL_KEY] ?: AppSettings().model,
                temperature = prefs[TEMPERATURE_KEY] ?: AppSettings().temperature,
                theme = prefs[THEME_KEY]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.System,
            )
        }

    override suspend fun updateModel(model: String) {
        dataStore.edit { it[MODEL_KEY] = model }
    }

    override suspend fun updateTemperature(temperature: Float) {
        dataStore.edit { it[TEMPERATURE_KEY] = temperature }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }

    private companion object {
        val MODEL_KEY = stringPreferencesKey("model")
        val TEMPERATURE_KEY = floatPreferencesKey("temperature")
        val THEME_KEY = stringPreferencesKey("theme")
    }
}
