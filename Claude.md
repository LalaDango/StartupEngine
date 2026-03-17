# 着手エンジン (Startup Engine)

## プロジェクト概要
先送りタスクをAIが超小ステップに分解するAndroidアプリ。
FastFlowLM (Qwen3-8B) バックエンド、OpenAI互換API使用。

## 必須ルール
- テスト: 各機能実装後に必ずビルド確認する
- UI: Jetpack Compose。ダークテーマ基調
- DB: Room。会話履歴はDBに保存しない（メモリのみ）
- API: 非ストリーミング。レスポンスは必ずJSONパーサーを通す
- resume: 会話履歴リセット + 完了ステップ圧縮メッセージのみ送信
- <think>タグ: JSONパース前に必ず除去する正規表現処理を入れる

## アーキテクチャ
UI(Compose) → ViewModel → Repository → Room DB / Retrofit

## ファイル構成
app/src/main/java/com/example/startupengine/
  ├── data/
  │   ├── db/ (Room entities, DAOs, Database)
  │   ├── api/ (Retrofit service, request/response models)
  │   └── repository/ (TaskRepository, AIRepository)
  ├── ui/
  │   ├── home/ (HomeScreen, HomeViewModel)
  │   ├── session/ (SessionScreen, SessionViewModel)
  │   ├── settings/ (SettingsScreen)
  │   └── theme/ (Color, Typography, Theme)
  └── util/ (JSONParser, TokenEstimator)