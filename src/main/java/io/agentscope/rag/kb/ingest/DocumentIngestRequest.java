package io.agentscope.rag.kb.ingest;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class DocumentIngestRequest {

    @NotBlank
    private String docId;

    private String title;

    @NotBlank
    private String text;

    private Map<String, Object> payload;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String buildBodyText() {
        if (title == null || title.isBlank()) {
            return text;
        }
        return title.strip() + "\n\n" + text.strip();
    }
}
