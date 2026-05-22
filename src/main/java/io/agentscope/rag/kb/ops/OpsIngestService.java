package io.agentscope.rag.kb.ops;

import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.reader.PDFReader;
import io.agentscope.core.rag.reader.Reader;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.reader.WordReader;
import io.agentscope.rag.kb.config.OpsProperties;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.ingest.DocumentIngestRequest;
import io.agentscope.rag.kb.ingest.IngestResult;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OpsIngestService {

    private static final Logger log = LoggerFactory.getLogger(OpsIngestService.class);

    private final KnowledgeBaseRegistry registry;
    private final TextReader textReader;
    private final WordReader wordReader;
    private final PDFReader pdfReader;
    private final KbIndexRegistry kbIndexRegistry;
    private final OpsProperties opsProperties;

    public OpsIngestService(
            KnowledgeBaseRegistry registry,
            TextReader kbTextReader,
            WordReader kbWordReader,
            PDFReader kbPdfReader,
            KbIndexRegistry kbIndexRegistry,
            OpsProperties opsProperties) {
        this.registry = registry;
        this.textReader = kbTextReader;
        this.wordReader = kbWordReader;
        this.pdfReader = kbPdfReader;
        this.kbIndexRegistry = kbIndexRegistry;
        this.opsProperties = opsProperties;
    }

    public IngestResult ingestText(String kbId, DocumentIngestRequest request, boolean replaceExisting) {
        return ingestText(kbId, request, replaceExisting, MaterialType.TEXT);
    }

    public IngestResult ingestText(
            String kbId, DocumentIngestRequest request, boolean replaceExisting, MaterialType materialType) {
        KnowledgeBaseContext ctx = registry.require(kbId);
        String docId = request.getDocId().trim();
        long deleted = replaceExisting ? deleteExisting(ctx, docId) : 0;

        List<Document> rawChunks =
                textReader.read(ReaderInput.fromString(request.buildBodyText())).blockOptional().orElse(List.of());
        if (rawChunks.isEmpty()) {
            throw new IllegalArgumentException("No chunks produced for docId=" + docId);
        }

        Map<String, Object> payload = buildPayload(request, materialType, null);
        List<Document> chunks = bindChunks(docId, rawChunks, payload);
        ctx.knowledge().addDocuments(chunks).block();
        kbIndexRegistry.recordIngest(1, chunks.size());
        log.info("Ingested kb={} docId={} chunks={} material={}", kbId, docId, chunks.size(), materialType);
        return new IngestResult(docId, chunks.size(), deleted);
    }

    public IngestResult ingestFile(
            String kbId,
            String docId,
            String title,
            MaterialType materialType,
            MultipartFile file,
            boolean replaceExisting,
            Map<String, Object> extraPayload)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        KnowledgeBaseContext ctx = registry.require(kbId);
        String normalizedDocId = docId != null && !docId.isBlank() ? docId.trim() : stripExtension(file.getOriginalFilename());
        long deleted = replaceExisting ? deleteExisting(ctx, normalizedDocId) : 0;

        Path saved = saveUpload(file);
        try {
            Reader reader = resolveReader(materialType);
            List<Document> rawChunks =
                    reader.read(ReaderInput.fromPath(saved)).blockOptional().orElse(List.of());
            if (rawChunks.isEmpty()) {
                throw new IllegalArgumentException("No chunks produced from file for docId=" + normalizedDocId);
            }

            Map<String, Object> payload = new HashMap<>();
            if (extraPayload != null) {
                payload.putAll(extraPayload);
            }
            payload.put("doc_id", normalizedDocId);
            payload.put("material_type", materialType.name());
            payload.put("reader", materialType.getReaderLabel());
            payload.put("source_file", file.getOriginalFilename());
            payload.put("ingested_at", Instant.now().toString());
            if (title != null && !title.isBlank()) {
                payload.put("title", title);
            }

            List<Document> chunks = bindChunks(normalizedDocId, rawChunks, payload);
            ctx.knowledge().addDocuments(chunks).block();
            kbIndexRegistry.recordIngest(1, chunks.size());
            log.info(
                    "File ingested kb={} docId={} chunks={} material={} file={}",
                    kbId,
                    normalizedDocId,
                    chunks.size(),
                    materialType,
                    file.getOriginalFilename());
            return new IngestResult(normalizedDocId, chunks.size(), deleted);
        } finally {
            Files.deleteIfExists(saved);
        }
    }

    public long deleteDocument(String kbId, String docId) {
        KnowledgeBaseContext ctx = registry.require(kbId);
        return deleteExisting(ctx, docId);
    }

    private long deleteExisting(KnowledgeBaseContext ctx, String docId) {
        return ctx.maintenance()
                .map(m -> m.deleteByDocId(docId))
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Delete is only supported for elasticsearch knowledge bases"));
    }

    private List<Document> bindChunks(String docId, List<Document> rawChunks, Map<String, Object> payload) {
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
        return chunks;
    }

    private Map<String, Object> buildPayload(
            DocumentIngestRequest request, MaterialType materialType, String sourceFile) {
        Map<String, Object> payload = new HashMap<>();
        if (request.getPayload() != null) {
            payload.putAll(request.getPayload());
        }
        payload.putIfAbsent("doc_id", request.getDocId());
        payload.put("material_type", materialType.name());
        payload.put("reader", materialType.getReaderLabel());
        payload.put("ingested_at", Instant.now().toString());
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            payload.putIfAbsent("title", request.getTitle());
        }
        if (sourceFile != null) {
            payload.put("source_file", sourceFile);
        }
        return payload;
    }

    private Reader resolveReader(MaterialType materialType) {
        return switch (materialType) {
            case TEXT -> textReader;
            case WORD -> wordReader;
            case PDF -> pdfReader;
        };
    }

    private Path saveUpload(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = Path.of(opsProperties.getUploadDir());
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "_" + safeName);
        file.transferTo(target);
        return target;
    }

    private static String stripExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "doc-" + UUID.randomUUID();
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
