package com.ruby.myllmchatkmp.data.network

import com.ruby.myllmchatkmp.domain.model.ChatEvent
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Talks to an OpenAI-compatible `/chat/completions` endpoint and streams the SSE response.
 * Cancelling collection of the returned [Flow] cancels the underlying HTTP call, since `execute`
 * runs inside the collector's coroutine and Ktor honors coroutine cancellation.
 */
class ChatCompletionsApi(
    private val httpClient: HttpClient,
    private val config: ChatApiConfig,
) {
    fun streamReply(messages: List<ChatMessageDto>): Flow<ChatEvent> =
        channelFlow {
            try {
                httpClient
                    .preparePost("${config.baseUrl}/chat/completions") {
                        header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                        contentType(ContentType.Application.Json)
                        setBody(ChatCompletionRequestDto(model = config.model, messages = messages, stream = true))
                    }.execute { response ->
                        if (!response.status.isSuccess()) {
                            send(ChatEvent.Error(mapHttpStatusToChatError(response.status)))
                            return@execute
                        }
                        val parser = ChatSseParser()
                        val channel = response.bodyAsChannel()
                        while (!channel.isClosedForRead) {
                            val line = channel.readUTF8Line() ?: break
                            for (event in parser.feed("$line\n")) {
                                send(event)
                                if (event is ChatEvent.Done) return@execute
                            }
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                send(ChatEvent.Error(mapThrowableToChatError(e)))
            }
        }
}
