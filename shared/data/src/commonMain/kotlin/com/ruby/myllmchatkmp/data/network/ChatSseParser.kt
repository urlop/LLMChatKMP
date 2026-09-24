package com.ruby.myllmchatkmp.data.network

import com.ruby.myllmchatkmp.domain.model.ChatError
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import kotlinx.serialization.json.Json

/**
 * Buffers raw SSE text into lines -- tolerating chunk boundaries that split mid-line -- and maps
 * `data: ...` payloads to [ChatEvent]s. One instance per stream; not thread-safe or reusable.
 */
class ChatSseParser(
    private val json: Json = defaultJson,
) {
    private var buffer = ""

    /** Feeds a raw chunk of the SSE body; returns the [ChatEvent]s any newly completed lines produced. */
    fun feed(rawChunk: String): List<ChatEvent> {
        buffer += rawChunk
        val events = mutableListOf<ChatEvent>()
        while (true) {
            val newlineIndex = buffer.indexOf('\n')
            if (newlineIndex == -1) break
            val line = buffer.substring(0, newlineIndex).trimEnd('\r')
            buffer = buffer.substring(newlineIndex + 1)
            parseLine(line)?.let { events.add(it) }
        }
        return events
    }

    private fun parseLine(line: String): ChatEvent? {
        if (line.isBlank()) return null
        if (!line.startsWith("data:")) return null
        val payload = line.removePrefix("data:").trim()
        if (payload == "[DONE]") return ChatEvent.Done
        return try {
            val chunk = json.decodeFromString(ChatCompletionChunkDto.serializer(), payload)
            val content =
                chunk.choices
                    .firstOrNull()
                    ?.delta
                    ?.content
            if (content.isNullOrEmpty()) null else ChatEvent.Delta(content)
        } catch (e: Exception) {
            ChatEvent.Error(ChatError.Unknown("Malformed SSE payload: ${e.message}"))
        }
    }
}
