package io.agentscope.rag.kb.web.dto;

public class DocumentIngestResponse {

    private String docId;
    private int chunkCount;
    private long deletedChunks;

    public DocumentIngestResponse(String docId, int chunkCount, long deletedChunks) {
        this.docId = docId;
        this.chunkCount = chunkCount;
        this.deletedChunks = deletedChunks;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public long getDeletedChunks() {
        return deletedChunks;
    }

    public void setDeletedChunks(long deletedChunks) {
        this.deletedChunks = deletedChunks;
    }
}
