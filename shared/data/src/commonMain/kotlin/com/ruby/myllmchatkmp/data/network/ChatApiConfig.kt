package com.ruby.myllmchatkmp.data.network

/** Targets an OpenAI-compatible `/chat/completions` endpoint (OpenAI, Groq, Together, Ollama, ...). */
data class ChatApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val temperature: Float? = null,
)
