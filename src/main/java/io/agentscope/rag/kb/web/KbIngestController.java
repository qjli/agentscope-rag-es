package io.agentscope.rag.kb.web;

import io.agentscope.rag.kb.ingest.DocumentIngestRequest;
import io.agentscope.rag.kb.ingest.IngestResult;
import io.agentscope.rag.kb.ingest.IngestService;
import io.agentscope.rag.kb.web.dto.DocumentIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kb")
@Tag(name = "Knowledge Ingest", description = "文档入库（SimpleKnowledge → ElasticsearchStore）")
public class KbIngestController {

    private final IngestService ingestService;

    public KbIngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "入库文档", description = "doc_id + 正文 + 元数据 → 分块 → 向量化 → ES")
    public DocumentIngestResponse ingest(@Valid @RequestBody DocumentIngestRequest request) {
        IngestResult result = ingestService.ingest(request);
        return new DocumentIngestResponse(result.docId(), result.chunkCount(), result.deletedChunks());
    }

    @PutMapping(value = "/documents/{docId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "覆盖更新文档", description = "先按 doc_id 删除 ES chunk，再重新入库")
    public DocumentIngestResponse update(
            @PathVariable String docId, @Valid @RequestBody DocumentIngestRequest request) {
        request.setDocId(docId);
        IngestResult result = ingestService.ingest(request, true);
        return new DocumentIngestResponse(result.docId(), result.chunkCount(), result.deletedChunks());
    }

    @DeleteMapping(value = "/documents/{docId}")
    @Operation(summary = "按 doc_id 删除全部 chunk")
    public DocumentIngestResponse delete(@PathVariable String docId) {
        long deleted = ingestService.deleteDocument(docId);
        return new DocumentIngestResponse(docId, 0, deleted);
    }
}
