package com.ruby.myllmchatkmp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MyLLMChatKMP",
    ) {
        App()
    }
}