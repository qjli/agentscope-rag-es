package io.agentscope.rag.kb.ops.dto;

import java.util.List;

public record KbDashboardResponse(
        String knowledgeBaseId,
        String displayName,
        String indexName,
        String storeType,
        String elasticsearchUrl,
        boolean elasticsearchPing,
        long chunkCount,
        long documentCount,
        List<MaterialStat> materialDistribution) {}
