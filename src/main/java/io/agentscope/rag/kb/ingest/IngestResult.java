package io.agentscope.rag.kb.ingest;

public record IngestResult(String docId, int chunkCount, long deletedChunks) {}
