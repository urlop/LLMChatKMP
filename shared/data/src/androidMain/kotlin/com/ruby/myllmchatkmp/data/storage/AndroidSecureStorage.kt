package com.ruby.myllmchatkmp.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidSecureStorage(
    context: Context,
) : SecureStorage {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    override fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)

    override fun setApiKey(key: String?) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    private companion object {
        const val KEY_API_KEY = "api_key"
    }
}
