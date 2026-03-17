package com.example.startupengine.data.repository

import com.example.startupengine.data.api.AIResponse
import com.example.startupengine.data.api.ApiClient
import com.example.startupengine.data.api.ChatMessage
import com.example.startupengine.data.api.ChatRequest
import com.example.startupengine.data.db.CompletedStep
import com.example.startupengine.util.JsonParser
import com.example.startupengine.util.TokenEstimator

class AIRepository(private val settingsRepository: SettingsRepository) {

    companion object {
        private const val SYSTEM_PROMPT = """/no_think あなたは「着手エンジン」のAIアシスタントです。ユーザーが先送りしているタスクを、今すぐ着手できる超小ステップに分解する専門家です。

## 最重要ルール
1. 必ず純粋なJSONのみで応答する。マークダウンのコードブロック、説明文、前置きは一切含めない
2. ステップは必ず5分以内で完了できるものにする。5分を超える場合はさらに分解する
3. ユーザーに曖昧な質問を絶対にしない。「詳しく教えてください」は禁止。聞くなら「はい/いいえ」で答えられる具体的な質問にする
4. 情報が不足していても、まず最初の1ステップを提案する。完璧な計画は不要
5. ステップは行動レベルで具体的にする。「調べる」→「Googleで"○○ △△市 申請方法"と検索する」
6. ユーザーが提案中のステップを飛ばして先の行動を報告した場合、報告された行動以前のステップは全て完了済みとして扱い、現在地点から先の新しいステップを提案する

## 応答JSON形式

通常の応答（ステップ提案時）:
{"message":"短い応答2-3文。共感・励まし含む","next_step":"今すぐできる具体的な1ステップ（5分以内。行動レベルで明確に）","step_time":"推定時間（例: 2分、5分。5分を超えてはならない）","why_easy":"なぜこのステップが簡単かの一言（心理的ハードルを下げる）","needs_input":false}

情報確認が必要な場合（具体的にピンポイントで聞く）:
{"message":"はい/いいえで答えられる具体的な質問","needs_input":true}

ユーザーが「疲れた」「休憩」「今日はここまで」等と言った場合:
{"message":"労いの言葉。ここまでの進捗を具体的に褒める","needs_input":true,"pause":true}

ユーザーが「めんどくさい」「それも無理」等と言った場合（さらに分解）:
→ 現在のステップをさらに小さく分解したnext_stepを提案する。例: 「Googleで検索」→「スマホのブラウザアプリを開くだけ」

## 分解の具体例

入力「年金の免除申請したい」→ 詳しく聞かない。即座に:
{"message":"年金免除、やろうと思っただけで前進です！","next_step":"スマホでGoogleを開いて「国民年金 免除 申請方法」と検索する","step_time":"2分","why_easy":"検索するだけ。申請はまだしなくていい","needs_input":false}

入力「副業探しがめんどう」→ 即座に:
{"message":"わかります。でも今日は「見るだけ」でOK。","next_step":"スマホでGoogleを開いて「副業 在宅 初心者」と検索する","step_time":"2分","why_easy":"検索結果を眺めるだけ。登録も応募もしない","needs_input":false}

「それもめんどくさい」→ さらに小さく:
{"message":"OK、もっと小さくしましょう。","next_step":"スマホのホーム画面にあるブラウザアプリのアイコンをタップして開く","step_time":"10秒","why_easy":"アイコンを1回タップするだけ","needs_input":false}

提案中「Googleで遅延情報を検索する」→ ユーザー「会社に遅れる連絡しました」→ 検索は飛ばされた。連絡済みを踏まえて先へ:
{"message":"素早い対応ですね！連絡済みなら安心です。","next_step":"遅延の間にできる作業を1つ決める（メール確認、資料の読み返しなど）","step_time":"2分","why_easy":"スマホで考えるだけ、何も始めなくていい","needs_input":false}

## 禁止事項
- 「詳しく教えてください」「どんな状況ですか？」のような漠然とした質問
- 5分を超えるステップの提案
- 全体計画や手順一覧の提示（次の1手だけ）
- タスク分解以外の会話（雑談、相談、情報提供）
- マークダウン記法、コードブロック、説明文の付加"""
    }

    private val conversationHistory = mutableListOf<ChatMessage>()

    fun resetHistory() {
        conversationHistory.clear()
    }

    fun buildResumeMessage(taskName: String, completedSteps: List<CompletedStep>): String {
        return if (completedSteps.isEmpty()) {
            "タスク「${taskName}」に取り組みます。最初のステップを1つ提案して。"
        } else {
            val stepsStr = completedSteps.mapIndexed { i, step ->
                "${i + 1}. ${step.stepText}"
            }.joinToString("\n")
            "タスク「${taskName}」の続き。完了済みステップ:\n$stepsStr\n次のステップを1つ提案して。"
        }
    }

    suspend fun sendMessage(userMessage: String): AIResponse {
        conversationHistory.add(ChatMessage(role = "user", content = userMessage))

        // コンテキストウィンドウチェック
        val contextWindow = settingsRepository.getContextWindowSize()
        trimHistoryIfNeeded(contextWindow)

        val allMessages = mutableListOf(
            ChatMessage(role = "system", content = SYSTEM_PROMPT)
        )
        allMessages.addAll(conversationHistory)

        return try {
            val baseUrl = settingsRepository.getBaseUrl()
            val modelName = settingsRepository.getModelName()
            val api = ApiClient.getApi(baseUrl)
            val request = ChatRequest(
                model = modelName,
                messages = allMessages
            )
            val response = api.chat(request)
            val rawText = response.choices.firstOrNull()?.message?.content ?: ""
            conversationHistory.add(ChatMessage(role = "assistant", content = rawText))
            JsonParser.parseAIResponse(rawText)
        } catch (e: java.net.SocketTimeoutException) {
            AIResponse(
                message = "接続がタイムアウトしました。LLMサーバーが起動しているか確認してください。",
                needsInput = true
            )
        } catch (e: java.net.ConnectException) {
            AIResponse(
                message = "接続できません。LLMサーバーが起動しているか、ネットワーク接続を確認してください。",
                needsInput = true
            )
        } catch (e: Exception) {
            AIResponse(
                message = "エラーが発生しました: ${e.message}",
                needsInput = true
            )
        }
    }

    private suspend fun trimHistoryIfNeeded(contextWindow: Int) {
        val systemTokens = TokenEstimator.estimate(SYSTEM_PROMPT) + 4
        val historyTokens = TokenEstimator.estimateMessages(conversationHistory)
        val totalTokens = systemTokens + historyTokens
        val threshold = (contextWindow * 0.85).toInt()

        if (totalTokens > threshold && conversationHistory.size > 6) {
            // 直近3往復（6メッセージ）を保持、残りを切り捨て
            val keepCount = 6
            val toRemove = conversationHistory.size - keepCount
            repeat(toRemove) { conversationHistory.removeFirst() }
        }
    }
}
