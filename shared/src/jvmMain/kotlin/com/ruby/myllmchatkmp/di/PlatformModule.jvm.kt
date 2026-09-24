package com.ruby.myllmchatkmp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ruby.myllmchatkmp.data.connectivity.ConnectivityObserver
import com.ruby.myllmchatkmp.data.connectivity.JvmConnectivityObserver
import com.ruby.myllmchatkmp.data.local.AppDatabase
import com.ruby.myllmchatkmp.data.settings.DATA_STORE_FILE_NAME
import com.ruby.myllmchatkmp.data.settings.createDataStore
import com.ruby.myllmchatkmp.data.storage.JvmSecureStorage
import com.ruby.myllmchatkmp.data.storage.SecureStorage
import com.ruby.myllmchatkmp.data.storage.appDataDir
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module =
    module {
        single<RoomDatabase.Builder<AppDatabase>> {
            Room.databaseBuilder<AppDatabase>(name = File(appDataDir(), "myllmchat.db").absolutePath)
        }
        single<DataStore<Preferences>> {
            createDataStore { File(appDataDir(), DATA_STORE_FILE_NAME).absolutePath }
        }
        single<SecureStorage> { JvmSecureStorage() }
        single<ConnectivityObserver> { JvmConnectivityObserver() }
    }
