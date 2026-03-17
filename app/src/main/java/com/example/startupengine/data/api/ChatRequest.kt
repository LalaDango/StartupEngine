package com.example.startupengine.data.api

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String = "qwen3:8b",
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 512,
    @SerializedName("temperature") val temperature: Double = 0.45,
    @SerializedName("top_p") val topP: Double = 0.9,
    @SerializedName("top_k") val topK: Int = 40,
    @SerializedName("repeat_penalty") val repeatPenalty: Double = 1.1,
    @SerializedName("frequency_penalty") val frequencyPenalty: Double = 0.2,
    @SerializedName("presence_penalty") val presencePenalty: Double = 0.0,
    @SerializedName("stream") val stream: Boolean = false
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)
