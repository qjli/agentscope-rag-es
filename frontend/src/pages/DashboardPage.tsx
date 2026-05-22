import { useEffect, useState } from "react";
import {
  Bar,
  BarChart,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Activity, Boxes, FileStack, Wifi } from "lucide-react";
import { api } from "../api/client";
import type { KbDashboard } from "../api/types";
import { useKb } from "../context/KbContext";

const COLORS = ["#0d9488", "#7c3aed", "#f59e0b", "#64748b"];

export function DashboardPage() {
  const { selectedKbId } = useKb();
  const [dash, setDash] = useState<KbDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setDash(null);
    setError(null);
    api
      .dashboard(selectedKbId)
      .then((data) => {
        if (!cancelled) setDash(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "加载失败");
      });
    return () => {
      cancelled = true;
    };
  }, [selectedKbId]);

  if (error) {
    return (
      <div className="rounded-3xl border border-red-200 bg-red-50 p-6 text-red-700">
        {error}
      </div>
    );
  }

  if (!dash) {
    return <div className="text-slate-500">加载 Dashboard…</div>;
  }

  const materialData =
    dash.materialDistribution?.map((m) => ({
      name: m.materialType,
      value: m.count,
    })) ?? [];

  const barData = [
    { label: "Chunks", value: Math.max(dash.chunkCount, 0) },
    { label: "Documents", value: Math.max(dash.documentCount, 0) },
  ];

  return (
    <div className="space-y-6">
      <div className="grid gap-5 lg:grid-cols-3">
        <div className="rounded-3xl bg-gradient-to-br from-teal-600 to-teal-700 p-6 text-white shadow-xl shadow-teal-600/25 lg:col-span-1">
          <p className="text-sm text-teal-100">向量 Chunk 总数</p>
          <p className="mt-2 text-4xl font-bold">{dash.chunkCount >= 0 ? dash.chunkCount : "—"}</p>
          <p className="mt-4 text-sm text-teal-100">索引 {dash.indexName}</p>
          <div className="mt-6 flex gap-2">
            <span className="rounded-xl bg-white/15 px-3 py-1 text-xs">
              {dash.storeType}
            </span>
            <span
              className={`rounded-xl px-3 py-1 text-xs ${dash.elasticsearchPing ? "bg-emerald-400/30" : "bg-red-400/30"}`}
            >
              ES {dash.elasticsearchPing ? "在线" : "离线"}
            </span>
          </div>
        </div>

        <StatCard
          icon={FileStack}
          label="业务文档数 (doc_id)"
          value={dash.documentCount >= 0 ? String(dash.documentCount) : "—"}
        />
        <StatCard
          icon={Wifi}
          label="Elasticsearch"
          value={dash.elasticsearchUrl.replace(/^https?:\/\//, "")}
          sub={dash.elasticsearchPing ? "连通正常" : "无法连接"}
        />
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        <div className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
          <div className="mb-4 flex items-center gap-2">
            <Activity className="h-5 w-5 text-violet-600" />
            <h3 className="font-semibold">入库规模</h3>
          </div>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={barData}>
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="value" fill="#0d9488" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
          <div className="mb-4 flex items-center gap-2">
            <Boxes className="h-5 w-5 text-teal-600" />
            <h3 className="font-semibold">物料来源分布</h3>
          </div>
          {materialData.length === 0 ? (
            <p className="py-16 text-center text-sm text-slate-400">暂无文档，请先入库</p>
          ) : (
            <div className="h-56">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={materialData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={4}
                  >
                    {materialData.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  sub,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
  sub?: string;
}) {
  return (
    <div className="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200/60">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-semibold text-slate-900">{value}</p>
          {sub && <p className="mt-1 text-xs text-slate-400">{sub}</p>}
        </div>
        <div className="rounded-2xl bg-slate-100 p-3">
          <Icon className="h-5 w-5 text-slate-600" />
        </div>
      </div>
    </div>
  );
}
