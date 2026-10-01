package com.ruby.myllmchatkmp.data.network

import com.ruby.myllmchatkmp.domain.model.ChatError
import com.ruby.myllmchatkmp.domain.model.ChatEvent
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
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
    private val configProvider: suspend () -> ChatApiConfig,
) {
    constructor(httpClient: HttpClient, config: ChatApiConfig) : this(httpClient, { config })

    fun streamReply(messages: List<ChatMessageDto>): Flow<ChatEvent> =
        channelFlow {
            try {
                // Resolved per call so key/model edits in Settings apply without restarting.
                val config = configProvider()
                httpClient
                    .preparePost("${config.baseUrl}/chat/completions") {
                        header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                        contentType(ContentType.Application.Json)
                        setBody(ChatCompletionRequestDto(model = config.model, messages = messages, stream = true, temperature = config.temperature))
                    }.execute { response ->
                        if (!response.status.isSuccess()) {
                            val body = runCatching { response.bodyAsText() }.getOrDefault("").take(500)
                            println("ChatHttp: ${response.status} from ${config.baseUrl} (model=${config.model}) body=$body")
                            val mapped = mapHttpStatusToChatError(response.status)
                            send(
                                ChatEvent.Error(
                                    if (mapped is ChatError.Unknown) ChatError.Unknown("${mapped.message}: $body") else mapped,
                                ),
                            )
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
                println("ChatHttp: request failed: $e")
                send(ChatEvent.Error(mapThrowableToChatError(e)))
            }
        }
}
