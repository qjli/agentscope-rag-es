import { useEffect, useState } from "react";
import { FileUp, RefreshCw, Trash2, Upload } from "lucide-react";
import { api } from "../api/client";
import type { KbDocumentRow, MaterialType } from "../api/types";
import { useKb } from "../context/KbContext";

const MATERIALS: { type: MaterialType; label: string; hint: string }[] = [
  { type: "TEXT", label: "TextReader", hint: ".txt / .md 纯文本" },
  { type: "WORD", label: "WordReader", hint: ".docx" },
  { type: "PDF", label: "PdfReader", hint: ".pdf" },
];

export function DocumentsPage() {
  const { selectedKbId } = useKb();
  const [rows, setRows] = useState<KbDocumentRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [docId, setDocId] = useState("");
  const [title, setTitle] = useState("");
  const [text, setText] = useState("");
  const [material, setMaterial] = useState<MaterialType>("TEXT");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);

  const load = () => {
    setLoading(true);
    api
      .documents(selectedKbId)
      .then(setRows)
      .catch((e) => setError(e instanceof Error ? e.message : "加载失败"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, [selectedKbId]);

  async function handleIngest(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (material === "TEXT") {
        await api.ingestText(selectedKbId, { docId, title, text });
      } else {
        if (!file) throw new Error("请选择文件");
        await api.ingestFile(selectedKbId, docId, material, file, title);
      }
      setText("");
      setFile(null);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "入库失败");
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(`删除 doc_id=${id} 的全部 chunk？`)) return;
    setBusy(true);
    try {
      await api.deleteDocument(selectedKbId, id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "删除失败");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid gap-6 xl:grid-cols-5">
      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60 xl:col-span-2">
        <h2 className="flex items-center gap-2 text-lg font-semibold">
          <Upload className="h-5 w-5 text-teal-600" />
          文档入库 / 覆盖
        </h2>
        <form className="mt-4 space-y-4" onSubmit={handleIngest}>
          <label className="block text-sm">
            <span className="text-slate-600">doc_id</span>
            <input
              required
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2"
              value={docId}
              onChange={(e) => setDocId(e.target.value)}
            />
          </label>
          <label className="block text-sm">
            <span className="text-slate-600">标题（可选）</span>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </label>
          <div>
            <span className="text-sm text-slate-600">物料类型</span>
            <div className="mt-2 grid grid-cols-3 gap-2">
              {MATERIALS.map((m) => (
                <button
                  key={m.type}
                  type="button"
                  onClick={() => setMaterial(m.type)}
                  className={`rounded-2xl border px-2 py-3 text-left text-xs transition ${
                    material === m.type
                      ? "border-teal-500 bg-teal-50 text-teal-800"
                      : "border-slate-200 hover:border-slate-300"
                  }`}
                >
                  <span className="font-semibold">{m.label}</span>
                  <span className="mt-1 block text-slate-400">{m.hint}</span>
                </button>
              ))}
            </div>
          </div>
          {material === "TEXT" ? (
            <label className="block text-sm">
              <span className="text-slate-600">正文</span>
              <textarea
                required
                rows={8}
                className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2 font-mono text-sm"
                value={text}
                onChange={(e) => setText(e.target.value)}
              />
            </label>
          ) : (
            <label className="flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50 py-8">
              <FileUp className="h-8 w-8 text-slate-400" />
              <span className="mt-2 text-sm text-slate-600">
                {file ? file.name : "点击选择文件"}
              </span>
              <input
                type="file"
                className="hidden"
                accept={
                  material === "WORD"
                    ? ".docx"
                    : material === "PDF"
                      ? ".pdf"
                      : ".txt,.md"
                }
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </label>
          )}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-2xl bg-teal-600 py-2.5 font-semibold text-white hover:bg-teal-700 disabled:opacity-60"
          >
            {busy ? "处理中…" : "入库（覆盖已有 doc_id）"}
          </button>
        </form>
      </section>

      <section className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60 xl:col-span-3">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">文档列表</h2>
          <button
            type="button"
            onClick={load}
            className="inline-flex items-center gap-1 rounded-xl border border-slate-200 px-3 py-1.5 text-sm hover:bg-slate-50"
          >
            <RefreshCw className="h-4 w-4" />
            刷新
          </button>
        </div>
        {loading ? (
          <p className="text-slate-500">加载中…</p>
        ) : rows.length === 0 ? (
          <p className="py-12 text-center text-slate-400">暂无文档</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-slate-500">
                  <th className="pb-3 pr-4">doc_id</th>
                  <th className="pb-3 pr-4">标题</th>
                  <th className="pb-3 pr-4">物料</th>
                  <th className="pb-3 pr-4">Chunks</th>
                  <th className="pb-3">操作</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.docId} className="border-b border-slate-50">
                    <td className="py-3 pr-4 font-mono text-xs">{row.docId}</td>
                    <td className="py-3 pr-4">{row.title}</td>
                    <td className="py-3 pr-4">
                      <span className="rounded-lg bg-violet-50 px-2 py-0.5 text-xs text-violet-700">
                        {row.materialType}
                      </span>
                    </td>
                    <td className="py-3 pr-4">{row.chunkCount}</td>
                    <td className="py-3">
                      <button
                        type="button"
                        onClick={() => handleDelete(row.docId)}
                        className="rounded-lg p-2 text-red-600 hover:bg-red-50"
                        title="删除全部 chunk"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
