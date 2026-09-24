package com.ruby.myllmchatkmp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MessageEntity?

    /** Run on app start: a message stuck mid-stream after a kill/crash can't still be streaming. */
    @Query("UPDATE messages SET status = 'Failed' WHERE status IN ('Streaming', 'Sending')")
    suspend fun markDanglingStreamsAsFailed()
}
