package com.example.startupengine.data.api

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("choices") val choices: List<Choice>
)

data class Choice(
    @SerializedName("message") val message: ChatMessage
)
