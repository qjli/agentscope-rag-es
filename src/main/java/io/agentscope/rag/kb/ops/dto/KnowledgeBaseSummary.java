package io.agentscope.rag.kb.ops.dto;

import java.time.Instant;

public record KnowledgeBaseSummary(
        String id,
        String displayName,
        String indexName,
        String description,
        boolean builtIn,
        Instant createdAt,
        boolean elasticsearchPing,
        long chunkCount,
        long documentCount) {}
