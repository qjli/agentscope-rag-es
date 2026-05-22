import type {
  ChatResponse,
  IngestResponse,
  KbDashboard,
  KbDocumentRow,
  KnowledgeBaseSummary,
  MaterialType,
} from "./types";

const API = "/api/v1/ops";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  listKnowledgeBases: () =>
    request<KnowledgeBaseSummary[]>(`${API}/knowledge-bases`),

  createKnowledgeBase: (body: {
    id: string;
    indexName: string;
    displayName?: string;
    description?: string;
  }) =>
    request<KnowledgeBaseSummary>(`${API}/knowledge-bases`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),

  dashboard: (kbId: string) =>
    request<KbDashboard>(`${API}/knowledge-bases/${kbId}/dashboard`),

  documents: (kbId: string, limit = 50) =>
    request<KbDocumentRow[]>(
      `${API}/knowledge-bases/${kbId}/documents?limit=${limit}`,
    ),

  ingestText: (
    kbId: string,
    body: { docId: string; title?: string; text: string },
  ) =>
    request<IngestResponse>(`${API}/knowledge-bases/${kbId}/documents`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),

  ingestFile: (
    kbId: string,
    docId: string,
    materialType: MaterialType,
    file: File,
    title?: string,
  ) => {
    const form = new FormData();
    form.append("file", file);
    return request<IngestResponse>(
      `${API}/knowledge-bases/${kbId}/documents/upload?docId=${encodeURIComponent(docId)}&materialType=${materialType}${title ? `&title=${encodeURIComponent(title)}` : ""}`,
      { method: "POST", body: form },
    );
  },

  deleteDocument: (kbId: string, docId: string) =>
    request<IngestResponse>(
      `${API}/knowledge-bases/${kbId}/documents/${encodeURIComponent(docId)}`,
      { method: "DELETE" },
    ),

  chat: (kbId: string, message: string, sessionId?: string) =>
    request<ChatResponse>(`${API}/knowledge-bases/${kbId}/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message, sessionId }),
    }),
};
