import { NavLink, Outlet } from "react-router-dom";
import {
  Database,
  FileText,
  LayoutDashboard,
  MessageCircle,
  Plus,
} from "lucide-react";
import { useState } from "react";
import { useKb } from "../context/KbContext";
import { CreateKbModal } from "./CreateKbModal";

const nav = [
  { to: "/", icon: LayoutDashboard, label: "Dashboard" },
  { to: "/documents", icon: FileText, label: "文档" },
  { to: "/chat", icon: MessageCircle, label: "AI 对话" },
];

export function Layout() {
  const { knowledgeBases, selectedKbId, setSelectedKbId, selectedKb } = useKb();
  const [showCreate, setShowCreate] = useState(false);

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-20 flex-col items-center gap-6 border-r border-slate-200/80 bg-white py-8 shadow-sm">
        <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-teal-500 to-violet-600 text-sm font-bold text-white shadow-lg shadow-teal-500/25">
          RAG
        </div>
        <nav className="flex flex-1 flex-col gap-3">
          {nav.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/"}
              className={({ isActive }) =>
                `group flex flex-col items-center gap-1 rounded-2xl px-2 py-3 text-xs transition ${
                  isActive
                    ? "bg-teal-50 text-teal-700"
                    : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
                }`
              }
            >
              <Icon className="h-5 w-5" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200/80 bg-white/80 px-8 py-5 backdrop-blur">
          <div>
            <p className="text-sm text-slate-500">RAG 运维控制台</p>
            <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
              {selectedKb?.displayName ?? "知识库"}
            </h1>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2">
              <Database className="h-4 w-4 text-teal-600" />
              <select
                className="bg-transparent text-sm font-medium outline-none"
                value={selectedKbId}
                onChange={(e) => setSelectedKbId(e.target.value)}
              >
                {knowledgeBases.map((kb) => (
                  <option key={kb.id} value={kb.id}>
                    {kb.displayName} ({kb.indexName})
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              onClick={() => setShowCreate(true)}
              className="inline-flex items-center gap-2 rounded-2xl bg-teal-600 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-teal-600/30 transition hover:bg-teal-700"
            >
              <Plus className="h-4 w-4" />
              新建知识库
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-auto p-8">
          <Outlet />
        </main>
      </div>

      {showCreate && <CreateKbModal onClose={() => setShowCreate(false)} />}
    </div>
  );
}
