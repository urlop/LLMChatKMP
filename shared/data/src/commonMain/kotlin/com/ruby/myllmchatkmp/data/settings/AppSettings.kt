package com.ruby.myllmchatkmp.data.settings

enum class AppTheme { Light, Dark, System }

data class AppSettings(
    val model: String = "gpt-4o-mini",
    val temperature: Float = 0.7f,
    val theme: AppTheme = AppTheme.System,
)
