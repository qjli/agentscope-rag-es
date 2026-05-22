package io.agentscope.rag.kb.faq;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class KbIndexRegistry {

    private final AtomicInteger documentCount = new AtomicInteger(0);
    private final AtomicInteger chunkCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastIngestAt = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public void recordIngest(int documents, int chunks) {
        documentCount.addAndGet(documents);
        chunkCount.addAndGet(chunks);
        lastIngestAt.set(Instant.now());
        lastError.set(null);
    }

    public void recordFaqLoad(int items, int chunks) {
        documentCount.set(items);
        chunkCount.set(chunks);
        lastIngestAt.set(Instant.now());
        lastError.set(null);
    }

    public void recordFailure(String error) {
        lastError.set(error);
    }

    public int getDocumentCount() {
        return documentCount.get();
    }

    public int getChunkCount() {
        return chunkCount.get();
    }

    public Instant getLastIngestAt() {
        return lastIngestAt.get();
    }

    public String getLastError() {
        return lastError.get();
    }

    /** 有 ES 文档或成功入库过即视为可用；精确数以 ES _count 为准。 */
    public boolean isReady() {
        return lastError.get() == null && (chunkCount.get() > 0);
    }

    public void resetCounts() {
        documentCount.set(0);
        chunkCount.set(0);
    }
}
