package com.ruby.myllmchatkmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ruby.myllmchatkmp.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MyLLMChatKMP",
        ) {
            App()
        }
    }
}
