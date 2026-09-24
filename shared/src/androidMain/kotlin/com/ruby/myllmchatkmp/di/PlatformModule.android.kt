package com.ruby.myllmchatkmp.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ruby.myllmchatkmp.data.connectivity.AndroidConnectivityObserver
import com.ruby.myllmchatkmp.data.connectivity.ConnectivityObserver
import com.ruby.myllmchatkmp.data.local.AppDatabase
import com.ruby.myllmchatkmp.data.settings.DATA_STORE_FILE_NAME
import com.ruby.myllmchatkmp.data.settings.createDataStore
import com.ruby.myllmchatkmp.data.storage.AndroidSecureStorage
import com.ruby.myllmchatkmp.data.storage.SecureStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<RoomDatabase.Builder<AppDatabase>> {
            val context = androidContext()
            Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath("myllmchat.db").absolutePath)
        }
        single<DataStore<Preferences>> {
            createDataStore { androidContext().filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath }
        }
        single<SecureStorage> { AndroidSecureStorage(androidContext()) }
        single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    }
