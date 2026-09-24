package com.ruby.myllmchatkmp.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Called once per process, before any Compose content that uses `koinInject`/`koinViewModel` is
 * shown. Android needs [declaration] to register its Context (`androidContext(this)`); iOS and
 * desktop can call this with no arguments.
 */
fun initKoin(declaration: KoinAppDeclaration = {}) {
    startKoin {
        declaration()
        modules(commonModule(), platformModule())
    }
}
