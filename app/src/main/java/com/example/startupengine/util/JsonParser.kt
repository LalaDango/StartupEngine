package com.example.startupengine.util

import com.example.startupengine.data.api.AIResponse
import com.google.gson.Gson
import com.google.gson.JsonObject

object JsonParser {
    private val gson = Gson()

    fun parseAIResponse(rawText: String): AIResponse {
        if (rawText.isBlank()) {
            return AIResponse(
                message = "応答を取得できませんでした。もう一度お試しください。",
                needsInput = true
            )
        }

        // Step 1: <think>...</think> タグを除去
        val noThink = rawText
            .replace(Regex("<think>[\\s\\S]*?</think>"), "")
            .trim()

        // Step 2: マークダウンコードブロックを除去
        val noCodeBlock = noThink
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        // Step 3: JSON部分を抽出
        val jsonMatch = Regex("\\{[\\s\\S]*\\}").find(noCodeBlock)
        val jsonStr = jsonMatch?.value ?: noCodeBlock

        // Step 4: JSONパース試行
        return try {
            val jsonObj = gson.fromJson(jsonStr, JsonObject::class.java)
            AIResponse(
                message = jsonObj.get("message")?.asString ?: "",
                needsInput = jsonObj.get("needs_input")?.asBoolean ?: false,
                nextStep = jsonObj.get("next_step")?.asString,
                stepTime = jsonObj.get("step_time")?.asString,
                whyEasy = jsonObj.get("why_easy")?.asString,
                pause = jsonObj.get("pause")?.asBoolean
            )
        } catch (e: Exception) {
            // Step 5: フォールバック
            AIResponse(
                message = noCodeBlock.ifEmpty { "応答の解析に失敗しました。もう一度お試しください。" },
                needsInput = true
            )
        }
    }
}
