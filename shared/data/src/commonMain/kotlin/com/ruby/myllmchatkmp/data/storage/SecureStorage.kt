package com.ruby.myllmchatkmp.data.storage

/**
 * Android Keystore-backed EncryptedSharedPreferences, iOS Keychain, a local file on JVM/desktop.
 * A plain interface (rather than expect/actual) because Android's implementation needs a
 * `Context` that the other platforms don't have -- each platform's Koin module binds its own
 * implementation instead (see PLAN.md phase 1 step 4 / phase 3 step 22).
 */
interface SecureStorage {
    fun getApiKey(): String?

    fun setApiKey(key: String?)
}
