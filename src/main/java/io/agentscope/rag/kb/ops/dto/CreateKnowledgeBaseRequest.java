package io.agentscope.rag.kb.ops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateKnowledgeBaseRequest {

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "[a-zA-Z0-9_-]+", message = "id may only contain letters, numbers, _ and -")
    private String id;

    @NotBlank
    @Size(max = 128)
    private String indexName;

    @Size(max = 128)
    private String displayName;

    @Size(max = 512)
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
