package io.agentscope.rag.kb.web.dto;

import java.util.List;

public class RetrieveResponse {

    private String query;
    private int count;
    private List<DocumentDto> documents;

    public RetrieveResponse(String query, List<DocumentDto> documents) {
        this.query = query;
        this.documents = documents;
        this.count = documents != null ? documents.size() : 0;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<DocumentDto> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentDto> documents) {
        this.documents = documents;
    }
}
