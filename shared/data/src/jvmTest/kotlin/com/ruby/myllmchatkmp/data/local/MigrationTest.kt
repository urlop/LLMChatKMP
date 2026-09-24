package com.ruby.myllmchatkmp.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ruby.myllmchatkmp.data.local.testfixtures.LegacyAppDatabaseV1
import com.ruby.myllmchatkmp.data.local.testfixtures.LegacyMessageEntityV1
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MigrationTest {
    private val dbFile = File.createTempFile("migration-test", ".db")

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    @Test
    fun migration1To2AddsTheModelColumnWithoutLosingExistingRows() =
        runTest {
            val legacyDb =
                Room
                    .databaseBuilder<LegacyAppDatabaseV1>(name = dbFile.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .build()
            legacyDb.legacyMessageDao().insert(
                LegacyMessageEntityV1(
                    id = "msg-1",
                    conversationId = "conv-1",
                    role = "User",
                    content = "hello",
                    status = "Done",
                    createdAt = 0L,
                ),
            )
            legacyDb.close()

            val migratedDb =
                Room
                    .databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .addMigrations(MIGRATION_1_2)
                    .build()

            val migrated = migratedDb.messageDao().getById("msg-1")
            assertEquals("hello", migrated?.content)
            assertNull(migrated?.model)
            migratedDb.close()
        }
}
