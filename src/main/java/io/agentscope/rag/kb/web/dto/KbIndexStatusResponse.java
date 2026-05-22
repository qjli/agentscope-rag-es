package io.agentscope.rag.kb.web.dto;

import java.time.Instant;

public class KbIndexStatusResponse {

    private boolean ready;
    private String storeType;
    private String indexName;
    private String elasticsearchUrl;
    private boolean elasticsearchPing;
    private long elasticsearchDocumentCount;
    private int lastIngestDocumentCount;
    private int lastIngestChunkCount;
    private Instant lastIngestAt;
    private String lastError;
    private String faqDataLocation;

    public KbIndexStatusResponse(
            boolean ready,
            String storeType,
            String indexName,
            String elasticsearchUrl,
            boolean elasticsearchPing,
            long elasticsearchDocumentCount,
            int lastIngestDocumentCount,
            int lastIngestChunkCount,
            Instant lastIngestAt,
            String lastError,
            String faqDataLocation) {
        this.ready = ready;
        this.storeType = storeType;
        this.indexName = indexName;
        this.elasticsearchUrl = elasticsearchUrl;
        this.elasticsearchPing = elasticsearchPing;
        this.elasticsearchDocumentCount = elasticsearchDocumentCount;
        this.lastIngestDocumentCount = lastIngestDocumentCount;
        this.lastIngestChunkCount = lastIngestChunkCount;
        this.lastIngestAt = lastIngestAt;
        this.lastError = lastError;
        this.faqDataLocation = faqDataLocation;
    }

    public boolean isReady() {
        return ready;
    }

    public String getStoreType() {
        return storeType;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getElasticsearchUrl() {
        return elasticsearchUrl;
    }

    public boolean isElasticsearchPing() {
        return elasticsearchPing;
    }

    public long getElasticsearchDocumentCount() {
        return elasticsearchDocumentCount;
    }

    public int getLastIngestDocumentCount() {
        return lastIngestDocumentCount;
    }

    public int getLastIngestChunkCount() {
        return lastIngestChunkCount;
    }

    public Instant getLastIngestAt() {
        return lastIngestAt;
    }

    public String getLastError() {
        return lastError;
    }

    public String getFaqDataLocation() {
        return faqDataLocation;
    }
}
