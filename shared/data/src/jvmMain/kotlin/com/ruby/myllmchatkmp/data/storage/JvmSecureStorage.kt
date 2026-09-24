package com.ruby.myllmchatkmp.data.storage

import java.io.File
import java.util.prefs.Preferences

/**
 * Desktop wasn't a roadmap target (Android + iOS were); this exists only because the KMP wizard
 * scaffolded a `desktopApp` module too. Uses the JDK's per-user `Preferences` store, which is
 * plaintext on most platforms -- fine for a demo build, not a real secrets store.
 */
class JvmSecureStorage : SecureStorage {
    private val prefs = Preferences.userNodeForPackage(JvmSecureStorage::class.java)

    override fun getApiKey(): String? = prefs.get(KEY_API_KEY, null)

    override fun setApiKey(key: String?) {
        if (key == null) prefs.remove(KEY_API_KEY) else prefs.put(KEY_API_KEY, key)
    }

    private companion object {
        const val KEY_API_KEY = "api_key"
    }
}

fun appDataDir(): File = File(System.getProperty("user.home"), ".myllmchatkmp").apply { mkdirs() }
