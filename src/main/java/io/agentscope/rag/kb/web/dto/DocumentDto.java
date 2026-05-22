package io.agentscope.rag.kb.web.dto;

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import java.util.Map;

public class DocumentDto {

    private String id;
    private Double score;
    private String content;
    private String docId;
    private String chunkId;
    private Map<String, Object> payload;

    public static DocumentDto from(Document document) {
        DocumentDto dto = new DocumentDto();
        dto.setId(document.getId());
        dto.setScore(document.getScore());
        DocumentMetadata metadata = document.getMetadata();
        if (metadata != null) {
            dto.setContent(metadata.getContentText());
            dto.setDocId(metadata.getDocId());
            dto.setChunkId(metadata.getChunkId());
            dto.setPayload(metadata.getPayload());
        }
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
