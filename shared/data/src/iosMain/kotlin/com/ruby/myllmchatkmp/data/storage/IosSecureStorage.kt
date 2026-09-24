package com.ruby.myllmchatkmp.data.storage

import platform.Foundation.NSUserDefaults

/**
 * TODO(needs Mac/Xcode to verify): this should be Keychain-backed (`SecItemAdd`/`SecItemCopyMatching`
 * from the Security framework), but that's low-level CoreFoundation interop this sandbox can only
 * compile, never run -- there's no macOS host or simulator here to confirm a read-back actually
 * works. Shipped as NSUserDefaults (unencrypted, app-sandboxed but NOT secure storage) so the app
 * still runs end-to-end on iOS; see PLAN.md phase 3 step 22 for what's needed to close this out.
 */
class IosSecureStorage : SecureStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getApiKey(): String? = defaults.stringForKey(KEY_API_KEY)

    override fun setApiKey(key: String?) {
        if (key == null) {
            defaults.removeObjectForKey(KEY_API_KEY)
        } else {
            defaults.setObject(key, KEY_API_KEY)
        }
    }

    private companion object {
        const val KEY_API_KEY = "api_key"
    }
}
