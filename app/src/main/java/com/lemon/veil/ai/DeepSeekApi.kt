package com.lemon.veil.ai

import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    val response_format: Map<String, String>? = null
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun getSuggestion(@Body request: ChatRequest): ChatResponse
}
