import { useState } from "react";
import { X } from "lucide-react";
import { useKb } from "../context/KbContext";

export function CreateKbModal({ onClose }: { onClose: () => void }) {
  const { createKb } = useKb();
  const [id, setId] = useState("");
  const [indexName, setIndexName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await createKb({ id, indexName, displayName, description });
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "创建失败");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">新建知识库</h2>
          <button type="button" onClick={onClose} className="rounded-xl p-2 hover:bg-slate-100">
            <X className="h-5 w-5" />
          </button>
        </div>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <label className="block text-sm">
            <span className="text-slate-600">知识库 ID</span>
            <input
              required
              pattern="[a-zA-Z0-9_-]+"
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2"
              value={id}
              onChange={(e) => setId(e.target.value)}
              placeholder="hr-kb"
            />
          </label>
          <label className="block text-sm">
            <span className="text-slate-600">ES index-name</span>
            <input
              required
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2"
              value={indexName}
              onChange={(e) => setIndexName(e.target.value)}
              placeholder="kb_hr_policies"
            />
          </label>
          <label className="block text-sm">
            <span className="text-slate-600">显示名称</span>
            <input
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </label>
          <label className="block text-sm">
            <span className="text-slate-600">描述</span>
            <textarea
              className="mt-1 w-full rounded-xl border border-slate-200 px-3 py-2"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </label>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-2xl bg-teal-600 py-2.5 font-semibold text-white hover:bg-teal-700 disabled:opacity-60"
          >
            {submitting ? "创建中…" : "创建"}
          </button>
        </form>
      </div>
    </div>
  );
}
