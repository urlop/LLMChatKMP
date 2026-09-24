package com.ruby.myllmchatkmp.data.repository

import com.ruby.myllmchatkmp.domain.model.ChatEvent
import com.ruby.myllmchatkmp.domain.model.Message
import com.ruby.myllmchatkmp.domain.model.MessageRole
import com.ruby.myllmchatkmp.domain.repository.ReplySource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/** Canned-text [ReplySource]: the app's default so it runs with no API key -- see PLAN.md phase 3 note. */
class FakeReplySource : ReplySource {
    override fun streamReply(
        conversationId: String,
        history: List<Message>,
    ): Flow<ChatEvent> =
        flow {
            val prompt = history.lastOrNull { it.role == MessageRole.User }?.content.orEmpty()
            val reply = cannedReply(prompt)
            val words = reply.split(" ")
            words.forEachIndexed { index, word ->
                delay(40)
                emit(ChatEvent.Delta(if (index == 0) word else " $word"))
            }
            emit(ChatEvent.Done)
        }

    private fun cannedReply(seed: String): String {
        val samples =
            listOf(
                "This is a canned reply from the fake reply source, streamed word by word so the UI can " +
                    "exercise streaming, cancel, and retry the same way a real backend would.",
                "Here's a demo response, persisted to the local database like a real one would be. Wire up " +
                    "a RemoteReplySource and API key when you're ready to talk to an actual model.",
                "Fake backend online. Streaming cursor, auto-scroll, retry, and offline history all work the " +
                    "same once a real ReplySource is swapped in via Koin.",
            )
        val index = Random(seed.hashCode()).nextInt(samples.size)
        return samples[index]
    }
}
