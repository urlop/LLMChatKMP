package com.ruby.myllmchatkmp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruby.myllmchatkmp.data.settings.AppSettings
import com.ruby.myllmchatkmp.data.settings.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    apiKeyDraft: String,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", modifier = Modifier.semantics { contentDescription = "Back" })
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = apiKeyDraft,
                onValueChange = onApiKeyChange,
                label = { Text("API key") },
                modifier = Modifier.fillMaxWidth(),
            )

            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.padding(8.dp))

            OutlinedTextField(
                value = settings.model,
                onValueChange = onModelChange,
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Temperature: ${settings.temperature}")
            Slider(value = settings.temperature, onValueChange = onTemperatureChange, valueRange = 0f..2f)

            Text("Theme")
            var expanded by remember { mutableStateOf(false) }
            TextButton(onClick = { expanded = true }) { Text(settings.theme.name) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppTheme.entries.forEach { theme ->
                    DropdownMenuItem(text = { Text(theme.name) }, onClick = {
                        onThemeChange(theme)
                        expanded = false
                    })
                }
            }
        }
    }
}
