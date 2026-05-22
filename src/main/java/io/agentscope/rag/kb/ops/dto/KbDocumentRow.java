package io.agentscope.rag.kb.ops.dto;

public record KbDocumentRow(
        String docId,
        String title,
        String materialType,
        long chunkCount,
        String sourceFile,
        String ingestedAt) {}
