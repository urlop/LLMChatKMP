package com.ruby.myllmchatkmp.data.local.testfixtures

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ruby.myllmchatkmp.data.local.ConversationEntity

/**
 * A frozen copy of the "messages" table as it looked at schema v1 (before the nullable `model`
 * column added in v2). Exists only so [com.ruby.myllmchatkmp.data.local.MIGRATION_1_2] can be
 * tested against a real v1 database file instead of guessed-at raw SQL -- see the migration test
 * under `data/src/jvmTest` and PLAN.md phase 3 step 24.
 */
@Entity(tableName = "messages")
data class LegacyMessageEntityV1(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val status: String,
    val createdAt: Long,
)

@Dao
interface LegacyMessageDao {
    @Insert
    suspend fun insert(message: LegacyMessageEntityV1)
}

@Database(entities = [ConversationEntity::class, LegacyMessageEntityV1::class], version = 1)
@ConstructedBy(LegacyAppDatabaseV1Constructor::class)
abstract class LegacyAppDatabaseV1 : RoomDatabase() {
    abstract fun legacyMessageDao(): LegacyMessageDao
}

@Suppress("KotlinNoActualForExpect")
expect object LegacyAppDatabaseV1Constructor : RoomDatabaseConstructor<LegacyAppDatabaseV1> {
    override fun initialize(): LegacyAppDatabaseV1
}
