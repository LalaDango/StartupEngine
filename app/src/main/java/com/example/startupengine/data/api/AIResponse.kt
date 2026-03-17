package com.example.startupengine.data.api

data class AIResponse(
    val message: String,
    val needsInput: Boolean = false,
    val nextStep: String? = null,
    val stepTime: String? = null,
    val whyEasy: String? = null,
    val pause: Boolean? = null
)
