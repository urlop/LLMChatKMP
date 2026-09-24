package com.ruby.myllmchatkmp.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

const val DATA_STORE_FILE_NAME = "myllmchat.preferences_pb"

/**
 * The file path needs a platform Context on Android and the documents dir on iOS, so (like the
 * Room builder) each platform's Koin module supplies [producePath] rather than this being an
 * expect/actual function -- see PLAN.md phase 1 step 4.
 */
fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
