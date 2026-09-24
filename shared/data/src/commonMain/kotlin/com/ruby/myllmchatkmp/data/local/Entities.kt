package com.ruby.myllmchatkmp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val status: String,
    val createdAt: Long,
    /** Added in schema v2 (see [com.ruby.myllmchatkmp.data.local.MIGRATION_1_2]); null for pre-existing rows. */
    val model: String? = null,
)
