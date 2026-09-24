package com.ruby.myllmchatkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ruby.myllmchatkmp.presentation.AppNavHost

/** Assumes [com.ruby.myllmchatkmp.di.initKoin] already ran -- see each platform's entry point. */
@Composable
@Preview
fun App() {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        AppNavHost()
    }
}
