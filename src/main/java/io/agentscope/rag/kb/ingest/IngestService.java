package io.agentscope.rag.kb.ingest;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.rag.kb.config.SimpleRagProperties;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final Knowledge kbKnowledge;
    private final TextReader kbTextReader;
    private final KbIndexRegistry registry;
    private final SimpleRagProperties properties;
    private final Optional<ElasticsearchDocMaintenance> esMaintenance;

    public IngestService(
            Knowledge kbKnowledge,
            TextReader kbTextReader,
            KbIndexRegistry registry,
            SimpleRagProperties properties,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.kbKnowledge = kbKnowledge;
        this.kbTextReader = kbTextReader;
        this.registry = registry;
        this.properties = properties;
        this.esMaintenance = Optional.ofNullable(esMaintenance);
    }

    public IngestResult ingest(DocumentIngestRequest request) {
        return ingest(request, true);
    }

    public IngestResult ingest(DocumentIngestRequest request, boolean replaceExisting) {
        String docId = request.getDocId().trim();
        long deleted = 0;
        if (replaceExisting) {
            deleted = deleteExistingDoc(docId);
        }

        String body = request.buildBodyText();
        List<Document> rawChunks =
                kbTextReader.read(ReaderInput.fromString(body)).blockOptional().orElse(List.of());
        if (rawChunks.isEmpty()) {
            throw new IllegalArgumentException("No chunks produced for docId=" + docId);
        }

        Map<String, Object> payload = buildPayload(request);
        List<Document> chunks = new ArrayList<>();
        for (int i = 0; i < rawChunks.size(); i++) {
            Document raw = rawChunks.get(i);
            DocumentMetadata meta =
                    DocumentMetadata.builder()
                            .content(raw.getMetadata().getContent())
                            .docId(docId)
                            .chunkId(String.valueOf(i))
                            .payload(payload)
                            .build();
            chunks.add(new Document(meta));
        }

        kbKnowledge.addDocuments(chunks).block();
        registry.recordIngest(1, chunks.size());
        log.info("Ingested docId={} chunks={} deletedBefore={}", docId, chunks.size(), deleted);
        return new IngestResult(docId, chunks.size(), deleted);
    }

    public long deleteDocument(String docId) {
        return deleteExistingDoc(docId);
    }

    private long deleteExistingDoc(String docId) {
        if (properties.getStoreType() == SimpleRagProperties.StoreType.ELASTICSEARCH) {
            return esMaintenance
                    .map(m -> m.deleteByDocId(docId))
                    .orElseThrow(() -> new IllegalStateException("Elasticsearch maintenance bean missing"));
        }
        log.warn("store-type=memory: deleteByDocId not supported, docId={} may duplicate on re-ingest", docId);
        return 0;
    }

    private Map<String, Object> buildPayload(DocumentIngestRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request.getPayload() != null) {
            payload.putAll(request.getPayload());
        }
        payload.putIfAbsent("doc_id", request.getDocId());
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            payload.putIfAbsent("title", request.getTitle());
        }
        return payload;
    }
}
