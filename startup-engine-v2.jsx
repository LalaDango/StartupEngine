import { useState, useRef, useEffect, useCallback } from "react";

const SYSTEM_PROMPT = `あなたは「着手エンジン」のAIアシスタントです。ユーザーが先送りしているタスクを、今すぐ着手できる超小ステップに分解する専門家です。

## あなたの役割
- ユーザーが漠然とした「やらなきゃ」を投げ込んだら、**今すぐできる5分以内の1ステップだけ**を提案する
- 全体像で圧倒しない。「次の1手」だけにフォーカスする
- 対話を通じてタスクの文脈を補完し、ユーザーが「なるほど、それならできる」と思える粒度まで分解する

## 応答フォーマット
必ず以下のJSON形式のみで応答してください。マークダウンのコードブロックで囲わず、純粋なJSONだけを返してください。

通常の応答:
{"message":"ユーザーへの短い応答（2-3文）","next_step":"今すぐできる具体的な1ステップ（5分以内）","step_time":"推定所要時間","why_easy":"なぜ簡単かの一言","needs_input":false}

情報が必要な場合:
{"message":"質問（フレンドリーに）","needs_input":true}

ユーザーが「疲れた」「休憩」「今日はここまで」と言った場合:
{"message":"労いの言葉と、ここまでの進捗を褒める内容（2-3文）","needs_input":true,"pause":true}

ユーザーが「再開」「続き」「前回の続き」と言った場合:
{"message":"おかえりの言葉と前回の振り返り（1-2文）","next_step":"次のステップ","step_time":"推定時間","why_easy":"なぜ簡単かの一言","needs_input":false}

## ルール
- 「まず調べましょう」ではなく「Googleで"○○ △△市"と検索」レベルの具体性
- ステップ完了ごとに次の1ステップだけ提案
- 「やる気が出ない」「面倒」には共感しつつ、さらに小さいステップに分解
- 行政タスクや副業・キャリア系タスクに特に詳しく対応`;

const T = {
  bg: "#0f0f1a",
  surface: "#181830",
  card: "#1e1e3a",
  cardHover: "#252548",
  accent: "#e94560",
  accentDim: "#e9456055",
  accentGlow: "#e9456033",
  success: "#4ade80",
  successDim: "#4ade8020",
  warn: "#fbbf24",
  text: "#eaeaea",
  sub: "#8892b0",
  dim: "#4a5270",
  border: "#2a2a4a",
};

// --- Storage helpers ---
async function loadTask(id) {
  try {
    const r = await window.storage.get(`task:${id}`);
    return r ? JSON.parse(r.value) : null;
  } catch { return null; }
}
async function saveTask(task) {
  try {
    await window.storage.set(`task:${task.id}`, JSON.stringify(task));
  } catch (e) { console.error("save failed", e); }
}
async function loadTaskList() {
  try {
    const r = await window.storage.get("task_list");
    return r ? JSON.parse(r.value) : [];
  } catch { return []; }
}
async function saveTaskList(list) {
  try {
    await window.storage.set("task_list", JSON.stringify(list));
  } catch (e) { console.error("save list failed", e); }
}
async function loadStats() {
  try {
    const r = await window.storage.get("global_stats");
    return r ? JSON.parse(r.value) : { totalSteps: 0, totalTasks: 0 };
  } catch { return { totalSteps: 0, totalTasks: 0 }; }
}
async function saveStats(stats) {
  try { await window.storage.set("global_stats", JSON.stringify(stats)); } catch {}
}

function parseAIResponse(text) {
  if (!text) return { message: "応答を取得できませんでした。もう一度試してください。", needs_input: true };
  const cleaned = text.replace(/```json\s*/g, "").replace(/```\s*/g, "").trim();
  // Try to find JSON object in text
  const jsonMatch = cleaned.match(/\{[\s\S]*\}/);
  if (jsonMatch) {
    try {
      return JSON.parse(jsonMatch[0]);
    } catch {}
  }
  // Fallback: treat as plain message
  return { message: cleaned, needs_input: true };
}

// --- Components ---

function TaskListItem({ task, onResume, onDelete }) {
  const stepCount = task.completedSteps?.length || 0;
  const isPaused = task.status === "paused";
  const dateStr = new Date(task.updatedAt || task.createdAt).toLocaleDateString("ja-JP", { month: "short", day: "numeric" });
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: "12px",
      padding: "14px 16px", background: T.card, borderRadius: "14px",
      border: `1px solid ${T.border}`, cursor: "pointer",
      transition: "all 0.2s",
    }}
      onClick={() => onResume(task)}
      onMouseEnter={e => { e.currentTarget.style.background = T.cardHover; e.currentTarget.style.borderColor = T.accent; }}
      onMouseLeave={e => { e.currentTarget.style.background = T.card; e.currentTarget.style.borderColor = T.border; }}
    >
      <div style={{
        width: "40px", height: "40px", borderRadius: "12px",
        background: isPaused ? T.accentGlow : T.successDim,
        display: "flex", alignItems: "center", justifyContent: "center",
        fontSize: "18px", flexShrink: 0,
      }}>
        {isPaused ? "⏸" : "✓"}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: "15px", fontWeight: 600, color: T.text,
          overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
        }}>
          {task.name}
        </div>
        <div style={{ fontSize: "12px", color: T.sub, marginTop: "2px" }}>
          {stepCount}ステップ完了 · {dateStr}
        </div>
      </div>
      {isPaused && (
        <div style={{
          padding: "5px 12px", borderRadius: "8px",
          background: T.accentDim, color: T.accent,
          fontSize: "12px", fontWeight: 700, flexShrink: 0,
        }}>
          続きから ▸
        </div>
      )}
      <button onClick={e => { e.stopPropagation(); onDelete(task.id); }} style={{
        background: "none", border: "none", color: T.dim, cursor: "pointer",
        fontSize: "16px", padding: "4px", flexShrink: 0,
      }}>×</button>
    </div>
  );
}

function CompletedStep({ step, index }) {
  return (
    <div style={{
      display: "flex", alignItems: "flex-start", gap: "10px",
      padding: "10px 14px", background: T.successDim,
      borderRadius: "10px", borderLeft: `3px solid ${T.success}`,
    }}>
      <div style={{
        width: "22px", height: "22px", borderRadius: "50%",
        background: T.success, display: "flex", alignItems: "center",
        justifyContent: "center", flexShrink: 0, fontSize: "12px", color: "#000", fontWeight: 700,
      }}>✓</div>
      <div>
        <div style={{ fontSize: "11px", color: T.sub }}>Step {index + 1}</div>
        <div style={{ fontSize: "14px", color: T.text, lineHeight: 1.5 }}>{step}</div>
      </div>
    </div>
  );
}

function StepCard({ step, time, whyEasy, onComplete }) {
  return (
    <div style={{
      padding: "18px", background: T.card,
      borderRadius: "16px", border: `2px solid ${T.accent}`,
      boxShadow: `0 0 20px ${T.accentGlow}`,
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "10px" }}>
        <span style={{ fontSize: "11px", fontWeight: 700, color: T.accent, letterSpacing: "0.1em", textTransform: "uppercase" }}>
          ▸ Next Step
        </span>
        <span style={{ fontSize: "11px", color: T.sub, background: T.accentGlow, padding: "2px 8px", borderRadius: "12px" }}>
          ⏱ {time}
        </span>
      </div>
      <div style={{ fontSize: "16px", fontWeight: 600, color: T.text, lineHeight: 1.6, marginBottom: "10px" }}>
        {step}
      </div>
      <div style={{ fontSize: "12px", color: T.sub, fontStyle: "italic", marginBottom: "14px" }}>
        💡 {whyEasy}
      </div>
      <button onClick={onComplete} style={{
        width: "100%", padding: "13px", borderRadius: "12px", border: "none",
        background: `linear-gradient(135deg, ${T.accent}, #c73e54)`,
        color: "#fff", fontSize: "15px", fontWeight: 700, cursor: "pointer",
      }}>
        ✓ できた！次へ
      </button>
    </div>
  );
}

function Bubble({ text, isAI }) {
  return (
    <div style={{ display: "flex", justifyContent: isAI ? "flex-start" : "flex-end" }}>
      <div style={{
        maxWidth: "85%", padding: "11px 15px",
        borderRadius: isAI ? "4px 14px 14px 14px" : "14px 4px 14px 14px",
        background: isAI ? T.card : T.accentDim,
        border: `1px solid ${isAI ? T.border : T.accent + "44"}`,
        color: T.text, fontSize: "14px", lineHeight: 1.6,
      }}>{text}</div>
    </div>
  );
}

// --- Main App ---
export default function StartupEngineV2() {
  const [view, setView] = useState("home"); // home | session
  const [tasks, setTasks] = useState([]);
  const [stats, setStats] = useState({ totalSteps: 0, totalTasks: 0 });
  const [currentTask, setCurrentTask] = useState(null);
  const [messages, setMessages] = useState([]);
  const [completedSteps, setCompletedSteps] = useState([]);
  const [currentStep, setCurrentStep] = useState(null);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [initLoading, setInitLoading] = useState(true);
  const scrollRef = useRef(null);
  const inputRef = useRef(null);
  const historyRef = useRef([]);

  // Load on mount
  useEffect(() => {
    (async () => {
      const [list, st] = await Promise.all([loadTaskList(), loadStats()]);
      setTasks(list); setStats(st); setInitLoading(false);
    })();
  }, []);

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages, currentStep, completedSteps]);

  useEffect(() => {
    if (view === "session") setTimeout(() => inputRef.current?.focus(), 100);
  }, [view]);

  const callAI = async (userMessage) => {
    historyRef.current.push({ role: "user", content: userMessage });
    try {
      const res = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514",
          max_tokens: 1000,
          system: SYSTEM_PROMPT,
          messages: historyRef.current,
        }),
      });
      const data = await res.json();
      const text = data.content?.filter(b => b.type === "text").map(b => b.text).join("") || "";
      historyRef.current.push({ role: "assistant", content: text });
      return parseAIResponse(text);
    } catch (err) {
      return { message: `接続エラー: ${err.message}`, needs_input: true };
    }
  };

  const persistTask = useCallback(async (task) => {
    await saveTask(task);
    const list = await loadTaskList();
    const idx = list.findIndex(t => t.id === task.id);
    const meta = { id: task.id, name: task.name, status: task.status, completedSteps: task.completedSteps, createdAt: task.createdAt, updatedAt: Date.now() };
    if (idx >= 0) list[idx] = meta; else list.unshift(meta);
    await saveTaskList(list);
    setTasks(list);
  }, []);

  // --- Session actions ---
  const startNewTask = (name) => {
    const task = {
      id: `t_${Date.now()}`, name, status: "active",
      completedSteps: [], conversationHistory: [],
      createdAt: Date.now(), updatedAt: Date.now(),
    };
    setCurrentTask(task);
    setMessages([]); setCompletedSteps([]); setCurrentStep(null);
    historyRef.current = [];
    setView("session");
    // Update stats
    const newStats = { ...stats, totalTasks: stats.totalTasks + 1 };
    setStats(newStats); saveStats(newStats);
    return task;
  };

  const resumeTask = async (taskMeta) => {
    const full = await loadTask(taskMeta.id);
    if (!full) return;
    setCurrentTask(full);
    setCompletedSteps(full.completedSteps || []);
    setMessages([]); setCurrentStep(null);
    historyRef.current = [];  // リセット：全履歴復元せず圧縮要約のみで再開
    setView("session");
    // Auto-ask AI to resume with compressed summary only
    setLoading(true);
    const stepsStr = (full.completedSteps || []).map((s, i) => `${i + 1}. ${s}`).join("\n");
    const resumeMsg = `タスク「${full.name}」の続き。完了済みステップ:\n${stepsStr}\n次のステップを1つ提案して。`;
    const result = await callAI(resumeMsg);
    if (result.next_step) {
      setMessages(prev => [...prev, { type: "ai", text: result.message }]);
      setCurrentStep({ step: result.next_step, time: result.step_time, whyEasy: result.why_easy });
    } else {
      setMessages(prev => [...prev, { type: "ai", text: result.message }]);
    }
    setLoading(false);
  };

  const deleteTask = async (id) => {
    try { await window.storage.delete(`task:${id}`); } catch {}
    const list = (await loadTaskList()).filter(t => t.id !== id);
    await saveTaskList(list);
    setTasks(list);
  };

  const handleSend = async () => {
    if (!input.trim() || loading) return;
    const text = input.trim(); setInput(""); setLoading(true);

    let task = currentTask;
    if (!task) { task = startNewTask(text); setCurrentTask(task); }

    setMessages(prev => [...prev, { type: "user", text }]);
    const result = await callAI(text);

    setMessages(prev => [...prev, { type: "ai", text: result.message }]);

    if (result.pause) {
      task.status = "paused";
      task.conversationHistory = historyRef.current;
      await persistTask(task);
      setCurrentTask(task);
    } else if (result.next_step) {
      setCurrentStep({ step: result.next_step, time: result.step_time, whyEasy: result.why_easy });
    }

    // Save conversation state
    if (task.id) {
      task.conversationHistory = historyRef.current;
      task.updatedAt = Date.now();
      await persistTask(task);
    }
    setLoading(false);
  };

  const handleComplete = async () => {
    if (!currentStep || loading) return;
    const newCompleted = [...completedSteps, currentStep.step];
    setCompletedSteps(newCompleted);
    setCurrentStep(null); setLoading(true);

    // Update stats
    const newStats = { ...stats, totalSteps: stats.totalSteps + 1 };
    setStats(newStats); saveStats(newStats);

    // Persist
    if (currentTask) {
      currentTask.completedSteps = newCompleted;
      currentTask.conversationHistory = historyRef.current;
      currentTask.updatedAt = Date.now();
      await persistTask(currentTask);
    }

    const result = await callAI("できた！次のステップを教えて");
    setMessages(prev => [...prev, { type: "ai", text: result.message }]);
    if (result.pause) {
      currentTask.status = "paused";
      await persistTask(currentTask);
    } else if (result.next_step) {
      setCurrentStep({ step: result.next_step, time: result.step_time, whyEasy: result.why_easy });
    }
    setLoading(false);
  };

  const handlePause = async () => {
    if (!currentTask) return;
    currentTask.status = "paused";
    currentTask.conversationHistory = historyRef.current;
    currentTask.updatedAt = Date.now();
    await persistTask(currentTask);
    setView("home");
    setCurrentTask(null); setCurrentStep(null);
    setMessages([]); setCompletedSteps([]);
    historyRef.current = [];
  };

  const goHome = () => {
    handlePause();
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  // --- RENDER ---
  if (initLoading) return (
    <div style={{ height: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: T.bg, color: T.sub }}>
      読み込み中...
    </div>
  );

  // HOME VIEW
  if (view === "home") {
    const pausedTasks = tasks.filter(t => t.status === "paused");
    const doneTasks = tasks.filter(t => t.status !== "paused");
    return (
      <div style={{
        height: "100vh", display: "flex", flexDirection: "column",
        background: T.bg, color: T.text,
        fontFamily: "'Segoe UI', 'Helvetica Neue', sans-serif",
      }}>
        {/* Header */}
        <div style={{ padding: "24px 20px 16px", textAlign: "center" }}>
          <div style={{ fontSize: "28px", marginBottom: "6px" }}>⚡</div>
          <div style={{ fontSize: "22px", fontWeight: 800, letterSpacing: "-0.02em" }}>
            <span style={{ color: T.accent }}>▸</span> 着手エンジン
          </div>
          <div style={{ fontSize: "13px", color: T.sub, marginTop: "6px" }}>
            先送りを、今すぐの1歩に
          </div>
          {/* Global stats */}
          <div style={{
            display: "flex", gap: "24px", justifyContent: "center",
            marginTop: "16px", padding: "12px", background: T.surface, borderRadius: "12px",
          }}>
            <div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: T.success }}>{stats.totalSteps}</div>
              <div style={{ fontSize: "10px", color: T.sub, letterSpacing: "0.08em" }}>TOTAL STEPS</div>
            </div>
            <div style={{ width: "1px", background: T.border }} />
            <div>
              <div style={{ fontSize: "20px", fontWeight: 800, color: T.accent }}>{stats.totalTasks}</div>
              <div style={{ fontSize: "10px", color: T.sub, letterSpacing: "0.08em" }}>TASKS</div>
            </div>
          </div>
        </div>

        {/* Task list */}
        <div style={{ flex: 1, overflowY: "auto", padding: "0 20px 20px" }}>
          {pausedTasks.length > 0 && (
            <>
              <div style={{ fontSize: "12px", color: T.sub, fontWeight: 700, letterSpacing: "0.08em", margin: "8px 0 10px", textTransform: "uppercase" }}>
                ⏸ 中断中のタスク
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginBottom: "20px" }}>
                {pausedTasks.map(t => (
                  <TaskListItem key={t.id} task={t} onResume={resumeTask} onDelete={deleteTask} />
                ))}
              </div>
            </>
          )}
          {doneTasks.length > 0 && (
            <>
              <div style={{ fontSize: "12px", color: T.sub, fontWeight: 700, letterSpacing: "0.08em", margin: "8px 0 10px", textTransform: "uppercase" }}>
                📋 履歴
              </div>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                {doneTasks.slice(0, 10).map(t => (
                  <TaskListItem key={t.id} task={t} onResume={resumeTask} onDelete={deleteTask} />
                ))}
              </div>
            </>
          )}
          {tasks.length === 0 && (
            <div style={{ textAlign: "center", padding: "40px 20px", color: T.dim }}>
              <div style={{ fontSize: "14px", lineHeight: 1.8 }}>
                「年金免除」「副業探し」「確定申告」...<br />
                曖昧でOK。まず1行投げ込んでみて。
              </div>
            </div>
          )}
        </div>

        {/* New task input */}
        <div style={{ padding: "12px 20px 20px", borderTop: `1px solid ${T.border}`, background: T.surface }}>
          <div style={{
            display: "flex", flexWrap: "wrap", gap: "6px", marginBottom: "10px",
          }}>
            {["年金の免除申請", "副業の方向性", "マイナンバー更新", "転職準備"].map(ex => (
              <button key={ex} onClick={() => { setInput(ex); inputRef.current?.focus(); }} style={{
                padding: "5px 12px", borderRadius: "16px", border: `1px solid ${T.border}`,
                background: T.card, color: T.sub, fontSize: "12px", cursor: "pointer",
              }}>
                {ex}
              </button>
            ))}
          </div>
          <div style={{ display: "flex", gap: "8px" }}>
            <input
              ref={inputRef} value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={e => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  if (input.trim()) { startNewTask(input.trim()); setInput(""); handleSendFromHome(input.trim()); }
                }
              }}
              placeholder="先送りしてるタスクを1行で..."
              style={{
                flex: 1, padding: "13px 16px", borderRadius: "12px",
                border: `1px solid ${T.border}`, background: T.bg,
                color: T.text, fontSize: "15px", outline: "none",
              }}
              onFocus={e => e.target.style.borderColor = T.accent}
              onBlur={e => e.target.style.borderColor = T.border}
            />
            <button onClick={() => {
              if (input.trim()) {
                const name = input.trim(); setInput("");
                const task = startNewTask(name);
                // Trigger first AI call
                setTimeout(async () => {
                  setLoading(true);
                  setMessages(prev => [...prev, { type: "user", text: name }]);
                  const result = await callAI(name);
                  setMessages(prev => [...prev, { type: "ai", text: result.message }]);
                  if (result.next_step) {
                    setCurrentStep({ step: result.next_step, time: result.step_time, whyEasy: result.why_easy });
                  }
                  task.conversationHistory = historyRef.current;
                  await persistTask(task);
                  setLoading(false);
                }, 50);
              }
            }} style={{
              padding: "13px 18px", borderRadius: "12px", border: "none",
              background: input.trim() ? T.accent : T.card,
              color: input.trim() ? "#fff" : T.dim,
              fontSize: "15px", fontWeight: 700, cursor: input.trim() ? "pointer" : "default",
              flexShrink: 0,
            }}>
              ▸
            </button>
          </div>
        </div>
      </div>
    );
  }

  // SESSION VIEW
  return (
    <div style={{
      height: "100vh", display: "flex", flexDirection: "column",
      background: T.bg, color: T.text,
      fontFamily: "'Segoe UI', 'Helvetica Neue', sans-serif",
      overflow: "hidden",
    }}>
      <style>{`
        @keyframes fadeIn { from { opacity:0; transform:translateY(6px); } to { opacity:1; transform:translateY(0); } }
        input::placeholder { color: ${T.dim}; }
      `}</style>

      {/* Session Header */}
      <div style={{
        padding: "12px 16px", borderBottom: `1px solid ${T.border}`,
        display: "flex", alignItems: "center", gap: "12px",
      }}>
        <button onClick={goHome} style={{
          background: "none", border: "none", color: T.sub, cursor: "pointer",
          fontSize: "18px", padding: "4px",
        }}>←</button>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: "15px", fontWeight: 700,
            overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
          }}>
            <span style={{ color: T.accent }}>▸</span> {currentTask?.name || "新しいタスク"}
          </div>
        </div>
        <div style={{
          display: "flex", alignItems: "center", gap: "8px",
        }}>
          <div style={{ textAlign: "center" }}>
            <div style={{ fontSize: "16px", fontWeight: 800, color: T.success }}>{completedSteps.length}</div>
            <div style={{ fontSize: "9px", color: T.sub }}>DONE</div>
          </div>
          <button onClick={handlePause} style={{
            padding: "6px 12px", borderRadius: "8px",
            border: `1px solid ${T.border}`, background: "transparent",
            color: T.warn, fontSize: "11px", fontWeight: 600, cursor: "pointer",
          }}>
            ⏸ 中断
          </button>
        </div>
      </div>

      {/* Messages area */}
      <div ref={scrollRef} style={{
        flex: 1, overflowY: "auto", padding: "16px",
        display: "flex", flexDirection: "column", gap: "10px",
      }}>
        {completedSteps.map((s, i) => <CompletedStep key={i} step={s} index={i} />)}
        {messages.map((m, i) => <Bubble key={i} text={m.text} isAI={m.type === "ai"} />)}
        {loading && (
          <div style={{ padding: "8px 0", color: T.sub, fontSize: "13px" }}>
            考え中...
          </div>
        )}
        {currentStep && <StepCard {...currentStep} onComplete={handleComplete} />}
      </div>

      {/* Input */}
      <div style={{ padding: "12px 16px", borderTop: `1px solid ${T.border}`, background: T.surface }}>
        <div style={{ display: "flex", gap: "8px" }}>
          <input
            ref={inputRef} value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={currentStep ? "困ったら何でも..." : "タスクを入力..."}
            disabled={loading}
            style={{
              flex: 1, padding: "12px 16px", borderRadius: "12px",
              border: `1px solid ${T.border}`, background: T.bg,
              color: T.text, fontSize: "14px", outline: "none",
            }}
            onFocus={e => e.target.style.borderColor = T.accent}
            onBlur={e => e.target.style.borderColor = T.border}
          />
          <button onClick={handleSend} disabled={loading || !input.trim()} style={{
            padding: "12px 18px", borderRadius: "12px", border: "none",
            background: input.trim() ? T.accent : T.card,
            color: input.trim() ? "#fff" : T.dim,
            fontSize: "14px", fontWeight: 700,
            cursor: input.trim() ? "pointer" : "default", flexShrink: 0,
          }}>▸</button>
        </div>
      </div>
    </div>
  );
}
