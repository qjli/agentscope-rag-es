package io.agentscope.rag.kb.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.rag.kb.config.SimpleRagProperties;
import io.agentscope.rag.kb.ops.dto.KbDashboardResponse;
import io.agentscope.rag.kb.ops.dto.KbDocumentRow;
import io.agentscope.rag.kb.ops.dto.KnowledgeBaseSummary;
import io.agentscope.rag.kb.ops.dto.MaterialStat;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpsDashboardService {

    private final KnowledgeBaseRegistry registry;
    private final SimpleRagProperties properties;
    private final ObjectMapper objectMapper;

    public OpsDashboardService(
            KnowledgeBaseRegistry registry, SimpleRagProperties properties, ObjectMapper objectMapper) {
        this.registry = registry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<KnowledgeBaseSummary> listKnowledgeBases() {
        List<KnowledgeBaseSummary> summaries = new ArrayList<>();
        for (KnowledgeBaseDescriptor descriptor : registry.listDescriptors()) {
            summaries.add(toSummary(descriptor));
        }
        return summaries;
    }

    public KbDashboardResponse dashboard(String kbId) {
        KnowledgeBaseContext ctx = registry.require(kbId);
        KnowledgeBaseDescriptor descriptor = ctx.descriptor();

        long chunkCount = -1;
        long docCount = -1;
        boolean esPing = false;
        if (ctx.maintenance().isPresent()) {
            ElasticsearchDocMaintenance maintenance = ctx.maintenance().get();
            chunkCount = maintenance.countDocuments();
            docCount = maintenance.countUniqueDocIds();
            esPing = maintenance.ping();
        }

        List<MaterialStat> materialStats = computeMaterialStats(ctx);
        return new KbDashboardResponse(
                descriptor.getId(),
                descriptor.getDisplayName(),
                descriptor.getIndexName(),
                properties.getStoreType().name(),
                properties.getElasticsearch().getUrl(),
                esPing,
                chunkCount,
                docCount,
                materialStats);
    }

    public List<KbDocumentRow> listDocuments(String kbId, int limit) {
        KnowledgeBaseContext ctx = registry.require(kbId);
        ElasticsearchDocMaintenance maintenance =
                ctx.maintenance()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Document list requires elasticsearch store"));
        JsonNode search = maintenance.searchDocuments(Math.min(Math.max(limit, 1), 200));
        List<KbDocumentRow> rows = new ArrayList<>();
        JsonNode buckets = search.path("aggregations").path("by_doc").path("buckets");
        if (!buckets.isArray()) {
            return rows;
        }
        for (JsonNode bucket : buckets) {
            String docId = bucket.path("key").asText();
            long chunkCount = bucket.path("chunk_count").path("value").asLong(0);
            String title = docId;
            String materialType = "TEXT";
            String sourceFile = null;
            String ingestedAt = null;
            JsonNode hits = bucket.path("sample").path("hits").path("hits");
            if (hits.isArray() && !hits.isEmpty()) {
                JsonNode source = hits.get(0).path("_source");
                JsonNode payloadNode = source.path("payload");
                if (payloadNode.isTextual()) {
                    try {
                        JsonNode payload = objectMapper.readTree(payloadNode.asText());
                        if (payload.hasNonNull("title")) {
                            title = payload.get("title").asText();
                        }
                        if (payload.hasNonNull("material_type")) {
                            materialType = payload.get("material_type").asText();
                        }
                        if (payload.hasNonNull("source_file")) {
                            sourceFile = payload.get("source_file").asText();
                        }
                        if (payload.hasNonNull("ingested_at")) {
                            ingestedAt = payload.get("ingested_at").asText();
                        }
                    } catch (Exception ignored) {
                        // ignore malformed payload
                    }
                }
            }
            rows.add(new KbDocumentRow(docId, title, materialType, chunkCount, sourceFile, ingestedAt));
        }
        return rows;
    }

    private KnowledgeBaseSummary toSummary(KnowledgeBaseDescriptor descriptor) {
        KnowledgeBaseContext ctx = registry.require(descriptor.getId());
        long chunks = ctx.maintenance().map(ElasticsearchDocMaintenance::countDocuments).orElse(-1L);
        long docs = ctx.maintenance().map(ElasticsearchDocMaintenance::countUniqueDocIds).orElse(-1L);
        boolean ping = ctx.maintenance().map(ElasticsearchDocMaintenance::ping).orElse(false);
        return new KnowledgeBaseSummary(
                descriptor.getId(),
                descriptor.getDisplayName(),
                descriptor.getIndexName(),
                descriptor.getDescription(),
                descriptor.isBuiltIn(),
                descriptor.getCreatedAt(),
                ping,
                chunks,
                docs);
    }

    private List<MaterialStat> computeMaterialStats(KnowledgeBaseContext ctx) {
        Map<String, Long> counts = new HashMap<>();
        for (KbDocumentRow row : listDocuments(ctx.descriptor().getId(), 100)) {
            counts.merge(row.materialType() != null ? row.materialType() : "TEXT", 1L, Long::sum);
        }
        List<MaterialStat> stats = new ArrayList<>();
        counts.forEach((type, count) -> stats.add(new MaterialStat(type, count)));
        return stats;
    }
}
