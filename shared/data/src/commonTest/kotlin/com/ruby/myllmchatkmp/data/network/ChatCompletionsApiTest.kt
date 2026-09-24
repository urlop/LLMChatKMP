package com.ruby.myllmchatkmp.data.network

import app.cash.turbine.test
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ChatCompletionsApiTest {
    private val sseBody =
        "data: {\"choices\":[{\"delta\":{\"content\":\"Hel\"}}]}\n\n" +
            "data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}\n\n" +
            "data: [DONE]\n\n"

    @Test
    fun streamsDeltaThenDoneFromAMockSseResponse() =
        runTest {
            val engine =
                MockEngine { request ->
                    respond(
                        content = ByteReadChannel(sseBody),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
                    )
                }
            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) { json(defaultJson) }
                }
            val api = ChatCompletionsApi(client, ChatApiConfig(baseUrl = "https://example.test", apiKey = "key", model = "test-model"))

            api.streamReply(listOf(ChatMessageDto(role = "user", content = "hi"))).test {
                assertEqualsEvent(ChatEvent.Delta("Hel"), awaitItem())
                assertEqualsEvent(ChatEvent.Delta("lo"), awaitItem())
                assertEqualsEvent(ChatEvent.Done, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun mapsAnUnauthorizedResponseToAChatError() =
        runTest {
            val engine =
                MockEngine {
                    respond(content = ByteReadChannel(""), status = HttpStatusCode.Unauthorized)
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json(defaultJson) } }
            val api = ChatCompletionsApi(client, ChatApiConfig(baseUrl = "https://example.test", apiKey = "bad", model = "test-model"))

            api.streamReply(listOf(ChatMessageDto(role = "user", content = "hi"))).test {
                val event = awaitItem()
                check(event is ChatEvent.Error) { "expected an error event, got $event" }
                awaitComplete()
            }
        }

    private fun assertEqualsEvent(
        expected: ChatEvent,
        actual: ChatEvent,
    ) {
        check(expected == actual) { "expected $expected but was $actual" }
    }
}
