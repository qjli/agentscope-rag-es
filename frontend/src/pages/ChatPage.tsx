import { useEffect, useState } from "react";
import { Bot, ChevronDown, ChevronUp, Search, Send, User } from "lucide-react";
import { api } from "../api/client";
import type { ChatResponse, KbDashboard, RetrievedChunk } from "../api/types";
import { useKb } from "../context/KbContext";

interface UserMessage {
  role: "user";
  text: string;
}

interface AssistantMessage {
  role: "assistant";
  query: string;
  answer: string;
  retrieved: RetrievedChunk[];
}

type Message = UserMessage | AssistantMessage;

function RetrievalPanel({
  query,
  chunks,
  defaultOpen,
}: {
  query: string;
  chunks: RetrievedChunk[];
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen ?? chunks.length > 0);

  return (
    <div className="mb-3 rounded-xl border border-teal-200/80 bg-teal-50/60">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between px-3 py-2 text-left text-xs font-semibold text-teal-800"
      >
        <span className="inline-flex items-center gap-1.5">
          <Search className="h-3.5 w-3.5" />
          检索命中 ({chunks.length})
        </span>
        {open ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
      </button>
      {open && (
        <div className="space-y-2 border-t border-teal-200/60 px-3 py-2">
          <p className="text-xs text-teal-700/80">查询：{query}</p>
          {chunks.length === 0 ? (
            <p className="text-xs text-slate-500">未命中知识库片段（可能低于 score 阈值）</p>
          ) : (
            chunks.map((c, idx) => (
              <div
                key={c.id ?? `${c.docId}-${c.chunkId}-${idx}`}
                className="rounded-lg bg-white/80 px-2.5 py-2 text-xs text-slate-700 ring-1 ring-teal-100"
              >
                <div className="mb-1 flex flex-wrap gap-2 text-[10px] text-slate-500">
                  {c.docId && (
                    <span className="font-mono">doc_id={c.docId}</span>
                  )}
                  {c.chunkId != null && <span>chunk={c.chunkId}</span>}
                  {c.score != null && (
                    <span className="text-teal-700">score={c.score.toFixed(3)}</span>
                  )}
                </div>
                <p className="whitespace-pre-wrap leading-relaxed">{c.content}</p>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export function ChatPage() {
  const { selectedKbId, selectedKb } = useKb();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [sessionId, setSessionId] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dash, setDash] = useState<KbDashboard | null>(null);

  useEffect(() => {
    api
      .dashboard(selectedKbId)
      .then(setDash)
      .catch(() => setDash(null));
  }, [selectedKbId]);

  const isEmpty = dash != null && dash.chunkCount === 0;

  function appendAssistant(res: ChatResponse) {
    setMessages((m) => [
      ...m,
      {
        role: "assistant",
        query: res.query ?? "",
        answer: res.answer,
        retrieved: res.retrievedDocuments ?? [],
      },
    ]);
  }

  async function send(e: React.FormEvent) {
    e.preventDefault();
    const text = input.trim();
    if (!text) return;
    if (isEmpty) {
      setError(
        `当前知识库 ES 索引 ${dash?.indexName ?? ""} 尚无数据，请先在「文档」页入库。`,
      );
      return;
    }
    setInput("");
    setMessages((m) => [...m, { role: "user", text }]);
    setLoading(true);
    setError(null);
    try {
      const res = await api.chat(selectedKbId, text, sessionId);
      if (res.sessionId) setSessionId(res.sessionId);
      appendAssistant(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "对话失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto flex h-[calc(100vh-8rem)] max-w-4xl flex-col rounded-3xl bg-white shadow-sm ring-1 ring-slate-200/60">
      <div className="border-b border-slate-100 px-6 py-4">
        <h2 className="font-semibold">AI 对话</h2>
        <p className="text-sm text-slate-500">
          当前知识库：<span className="font-medium text-teal-700">{selectedKb?.displayName}</span>
          （索引 <span className="font-mono">{selectedKb?.indexName}</span>）
          {dash != null && (
            <span className="ml-2">
              · {dash.chunkCount > 0 ? `${dash.chunkCount} chunks` : "暂无数据"}
            </span>
          )}
        </p>
        <p className="mt-1 text-xs text-slate-400">
          回答拆分为「检索命中」与「模型生成」两部分，便于核对 RAG 依据。
        </p>
        {isEmpty && (
          <p className="mt-2 rounded-xl bg-amber-50 px-3 py-2 text-sm text-amber-800">
            请先在「文档」页向本知识库入库；勿使用默认库 API，否则数据会写入 agentscope_kb。
          </p>
        )}
      </div>

      <div className="flex-1 space-y-4 overflow-y-auto px-6 py-4">
        {messages.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center text-slate-400">
            <Bot className="mb-3 h-12 w-12 text-violet-300" />
            <p>选择知识库后，先检索 ES，再由大模型生成回答</p>
          </div>
        )}
        {messages.map((msg, i) =>
          msg.role === "user" ? (
            <div key={i} className="flex justify-end gap-3">
              <div className="max-w-[80%] rounded-2xl bg-teal-600 px-4 py-3 text-sm leading-relaxed text-white">
                {msg.text}
              </div>
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-teal-100">
                <User className="h-5 w-5 text-teal-700" />
              </div>
            </div>
          ) : (
            <div key={i} className="flex gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-violet-100">
                <Bot className="h-5 w-5 text-violet-600" />
              </div>
              <div className="max-w-[85%] flex-1">
                <RetrievalPanel
                  query={msg.query}
                  chunks={msg.retrieved}
                  defaultOpen={msg.retrieved.length > 0}
                />
                <div className="rounded-2xl bg-violet-50 px-4 py-3 ring-1 ring-violet-100">
                  <p className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-violet-600">
                    模型回答
                  </p>
                  <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-800">
                    {msg.answer}
                  </p>
                </div>
              </div>
            </div>
          ),
        )}
        {loading && (
          <p className="text-sm text-slate-400">检索知识库 → 生成回答…</p>
        )}
      </div>

      {error && <p className="px-6 text-sm text-red-600">{error}</p>}

      <form
        onSubmit={send}
        className="flex gap-2 border-t border-slate-100 p-4"
      >
        <input
          className="flex-1 rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none focus:border-teal-500"
          placeholder="输入问题…"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={loading}
        />
        <button
          type="submit"
          disabled={loading || !input.trim()}
          className="inline-flex items-center gap-2 rounded-2xl bg-violet-600 px-5 py-3 font-semibold text-white hover:bg-violet-700 disabled:opacity-50"
        >
          <Send className="h-4 w-4" />
          发送
        </button>
      </form>
    </div>
  );
}
