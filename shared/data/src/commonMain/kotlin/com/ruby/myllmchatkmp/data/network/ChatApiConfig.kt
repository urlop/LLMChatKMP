package com.ruby.myllmchatkmp.data.network

/**
 * Targets an OpenAI-compatible `/chat/completions` endpoint (OpenAI, Groq, Together, a local
 * Ollama/LM Studio server, etc. all speak this shape). NEEDS A HUMAN DECISION: which provider to
 * point at and a real API key -- see PLAN.md step 11/16 notes. [com.ruby.myllmchatkmp.domain.fake.FakeChatRepository]
 * is the default Koin binding until that's supplied.
 */
data class ChatApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
)
