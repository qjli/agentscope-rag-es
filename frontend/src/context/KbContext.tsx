import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api } from "../api/client";
import type { KnowledgeBaseSummary } from "../api/types";

interface KbContextValue {
  knowledgeBases: KnowledgeBaseSummary[];
  selectedKbId: string;
  selectedKb: KnowledgeBaseSummary | undefined;
  loading: boolean;
  error: string | null;
  setSelectedKbId: (id: string) => void;
  /** 切换知识库（与 setSelectedKbId 相同，语义更明确） */
  selectKnowledgeBase: (id: string) => void;
  refresh: () => Promise<void>;
  createKb: (payload: {
    id: string;
    indexName: string;
    displayName?: string;
    description?: string;
  }) => Promise<void>;
}

const KbContext = createContext<KbContextValue | null>(null);

export function KbProvider({ children }: { children: ReactNode }) {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseSummary[]>(
    [],
  );
  const [selectedKbId, setSelectedKbId] = useState("default");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await api.listKnowledgeBases();
      setKnowledgeBases(list);
      if (list.length > 0 && !list.some((k) => k.id === selectedKbId)) {
        setSelectedKbId(list[0].id);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "加载知识库失败");
    } finally {
      setLoading(false);
    }
  }, [selectedKbId]);

  useEffect(() => {
    void refresh();
  }, []);

  const createKb = useCallback(
    async (payload: {
      id: string;
      indexName: string;
      displayName?: string;
      description?: string;
    }) => {
      await api.createKnowledgeBase(payload);
      await refresh();
      setSelectedKbId(payload.id);
    },
    [refresh],
  );

  const selectedKb = useMemo(
    () => knowledgeBases.find((k) => k.id === selectedKbId),
    [knowledgeBases, selectedKbId],
  );

  const selectKnowledgeBase = useCallback((id: string) => {
    setSelectedKbId(id);
  }, []);

  const value = useMemo(
    () => ({
      knowledgeBases,
      selectedKbId,
      selectedKb,
      loading,
      error,
      setSelectedKbId: selectKnowledgeBase,
      selectKnowledgeBase,
      refresh,
      createKb,
    }),
    [
      knowledgeBases,
      selectedKbId,
      selectedKb,
      loading,
      error,
      selectKnowledgeBase,
      refresh,
      createKb,
    ],
  );

  return <KbContext.Provider value={value}>{children}</KbContext.Provider>;
}

export function useKb() {
  const ctx = useContext(KbContext);
  if (!ctx) throw new Error("useKb must be used within KbProvider");
  return ctx;
}
