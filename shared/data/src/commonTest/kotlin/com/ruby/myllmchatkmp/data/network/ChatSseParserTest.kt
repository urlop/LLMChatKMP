package com.ruby.myllmchatkmp.data.network

import com.ruby.myllmchatkmp.domain.model.ChatEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatSseParserTest {
    @Test
    fun emitsDeltaForACompleteLine() {
        val parser = ChatSseParser()
        val events = parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}\n")
        assertEquals(listOf(ChatEvent.Delta("hi")), events)
    }

    @Test
    fun bufferAcrossAChunkSplitMidLine() {
        val parser = ChatSseParser()
        val firstHalf = parser.feed("data: {\"choices\":[{\"delta\":{\"conte")
        assertTrue(firstHalf.isEmpty())
        val secondHalf = parser.feed("nt\":\"hi\"}}]}\n")
        assertEquals(listOf(ChatEvent.Delta("hi")), secondHalf)
    }

    @Test
    fun ignoresEmptyLines() {
        val parser = ChatSseParser()
        val events = parser.feed("\n\ndata: {\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}\n\n")
        assertEquals(listOf(ChatEvent.Delta("hi")), events)
    }

    @Test
    fun emitsDoneOnDoneSentinel() {
        val parser = ChatSseParser()
        val events = parser.feed("data: [DONE]\n")
        assertEquals(listOf(ChatEvent.Done), events)
    }

    @Test
    fun emitsErrorForMalformedJson() {
        val parser = ChatSseParser()
        val events = parser.feed("data: {not json\n")
        assertEquals(1, events.size)
        assertIs<ChatEvent.Error>(events.first())
    }

    @Test
    fun handlesMultipleLinesInOneChunk() {
        val parser = ChatSseParser()
        val events =
            parser.feed(
                "data: {\"choices\":[{\"delta\":{\"content\":\"a\"}}]}\n" +
                    "data: {\"choices\":[{\"delta\":{\"content\":\"b\"}}]}\n" +
                    "data: [DONE]\n",
            )
        assertEquals(listOf(ChatEvent.Delta("a"), ChatEvent.Delta("b"), ChatEvent.Done), events)
    }
}
