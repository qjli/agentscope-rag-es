package io.agentscope.rag.kb.web.dto;

import java.util.Collections;
import java.util.List;

public class ChatResponse {

    private String query;
    private String answer;
    private String sessionId;
    private String knowledgeBaseId;
    private String indexName;
    private List<DocumentDto> retrievedDocuments;

    public ChatResponse(String answer, String sessionId) {
        this(null, answer, sessionId, null, null, Collections.emptyList());
    }

    public ChatResponse(String answer, String sessionId, String knowledgeBaseId, String indexName) {
        this(null, answer, sessionId, knowledgeBaseId, indexName, Collections.emptyList());
    }

    public ChatResponse(
            String query,
            String answer,
            String sessionId,
            String knowledgeBaseId,
            String indexName,
            List<DocumentDto> retrievedDocuments) {
        this.query = query;
        this.answer = answer;
        this.sessionId = sessionId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.indexName = indexName;
        this.retrievedDocuments =
                retrievedDocuments != null ? retrievedDocuments : Collections.emptyList();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public List<DocumentDto> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    public void setRetrievedDocuments(List<DocumentDto> retrievedDocuments) {
        this.retrievedDocuments = retrievedDocuments;
    }

    public int getRetrievedCount() {
        return retrievedDocuments != null ? retrievedDocuments.size() : 0;
    }
}
