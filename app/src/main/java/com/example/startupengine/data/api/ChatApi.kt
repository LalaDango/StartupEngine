package com.example.startupengine.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
