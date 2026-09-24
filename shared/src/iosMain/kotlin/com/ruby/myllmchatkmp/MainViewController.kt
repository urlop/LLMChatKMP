package com.ruby.myllmchatkmp

import androidx.compose.ui.window.ComposeUIViewController
import com.ruby.myllmchatkmp.di.initKoin
import platform.UIKit.UIViewController

private var koinStarted = false

// Capitalized to match the conventional iOS entry-point name Swift calls into.
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
