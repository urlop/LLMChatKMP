package com.ruby.myllmchatkmp.data.repository

import com.ruby.myllmchatkmp.data.local.ConversationEntity
import com.ruby.myllmchatkmp.data.local.MessageEntity
import com.ruby.myllmchatkmp.domain.model.Conversation
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.model.MessageStatus
import kotlin.time.Instant

fun ConversationEntity.toDomain() =
    Conversation(
        id = id,
        title = title,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

fun Conversation.toEntity() =
    ConversationEntity(
        id = id,
        title = title,
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
    )

fun MessageEntity.toDomain() =
    Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.valueOf(role),
        content = content,
        status = MessageStatus.valueOf(status),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
    )

fun Message.toEntity() =
    MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        content = content,
        status = status.name,
        createdAt = createdAt.toEpochMilliseconds(),
    )
