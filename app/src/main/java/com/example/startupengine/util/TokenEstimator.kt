package com.example.startupengine.util

object TokenEstimator {
    fun estimate(text: String): Int {
        var tokens = 0
        for (char in text) {
            tokens += if (char.code > 0x7F) 2 else 1
        }
        // ASCII部分は文字数ではなく単語数に近い概算（4文字≈1トークン）
        // 上記のループでは保守的に1文字1トークンで計算しているため、
        // 実際よりやや多めの見積もりとなる（安全側）
        return tokens
    }

    fun estimateMessages(messages: List<com.example.startupengine.data.api.ChatMessage>): Int {
        return messages.sumOf { estimate(it.content) + 4 } // 4 tokens overhead per message
    }
}
