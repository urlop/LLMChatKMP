package com.ruby.myllmchatkmp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ruby.myllmchatkmp.data.connectivity.ConnectivityObserver
import com.ruby.myllmchatkmp.data.connectivity.IosConnectivityObserver
import com.ruby.myllmchatkmp.data.local.AppDatabase
import com.ruby.myllmchatkmp.data.settings.DATA_STORE_FILE_NAME
import com.ruby.myllmchatkmp.data.settings.createDataStore
import com.ruby.myllmchatkmp.data.storage.IosSecureStorage
import com.ruby.myllmchatkmp.data.storage.SecureStorage
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun documentsDirectory(): String {
    val url =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(url?.path)
}

actual fun platformModule(): Module =
    module {
        single<RoomDatabase.Builder<AppDatabase>> {
            Room.databaseBuilder<AppDatabase>(name = documentsDirectory() + "/myllmchat.db")
        }
        single<DataStore<Preferences>> {
            createDataStore { documentsDirectory() + "/" + DATA_STORE_FILE_NAME }
        }
        single<SecureStorage> { IosSecureStorage() }
        single<ConnectivityObserver> { IosConnectivityObserver() }
    }
