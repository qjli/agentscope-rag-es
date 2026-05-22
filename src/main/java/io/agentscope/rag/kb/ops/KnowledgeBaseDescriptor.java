package io.agentscope.rag.kb.ops;

import java.time.Instant;

public class KnowledgeBaseDescriptor {

    private String id;
    private String displayName;
    private String indexName;
    private String description;
    private Instant createdAt;
    private boolean builtIn;

    public KnowledgeBaseDescriptor() {}

    public KnowledgeBaseDescriptor(
            String id,
            String displayName,
            String indexName,
            String description,
            Instant createdAt,
            boolean builtIn) {
        this.id = id;
        this.displayName = displayName;
        this.indexName = indexName;
        this.description = description;
        this.createdAt = createdAt;
        this.builtIn = builtIn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }
}
