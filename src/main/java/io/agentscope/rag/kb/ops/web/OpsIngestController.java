package io.agentscope.rag.kb.ops.web;

import io.agentscope.rag.kb.ingest.DocumentIngestRequest;
import io.agentscope.rag.kb.ingest.IngestResult;
import io.agentscope.rag.kb.ops.MaterialType;
import io.agentscope.rag.kb.ops.OpsIngestService;
import io.agentscope.rag.kb.web.dto.DocumentIngestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ops/knowledge-bases/{kbId}/documents")
@Tag(name = "Ops Ingest", description = "文本 / Word / PDF 入库与删除")
public class OpsIngestController {

    private final OpsIngestService ingestService;

    public OpsIngestController(OpsIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "文本入库（TextReader）")
    public DocumentIngestResponse ingestText(
            @PathVariable String kbId, @Valid @RequestBody DocumentIngestRequest request) {
        IngestResult result = ingestService.ingestText(kbId, request, true);
        return toResponse(result);
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "文件入库", description = "materialType: TEXT | WORD | PDF")
    public DocumentIngestResponse ingestFile(
            @PathVariable String kbId,
            @RequestParam String docId,
            @RequestParam MaterialType materialType,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title)
            throws IOException {
        IngestResult result = ingestService.ingestFile(kbId, docId, title, materialType, file, true, null);
        return toResponse(result);
    }

    @PutMapping(
            value = "/{docId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "覆盖更新（文本）")
    public DocumentIngestResponse updateText(
            @PathVariable String kbId,
            @PathVariable String docId,
            @Valid @RequestBody DocumentIngestRequest request) {
        request.setDocId(docId);
        IngestResult result = ingestService.ingestText(kbId, request, true);
        return toResponse(result);
    }

    @PutMapping(
            value = "/{docId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "覆盖更新（文件）")
    public DocumentIngestResponse updateFile(
            @PathVariable String kbId,
            @PathVariable String docId,
            @RequestParam MaterialType materialType,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title)
            throws IOException {
        IngestResult result = ingestService.ingestFile(kbId, docId, title, materialType, file, true, null);
        return toResponse(result);
    }

    @DeleteMapping(value = "/{docId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "按 doc_id 删除全部 chunk")
    public DocumentIngestResponse delete(@PathVariable String kbId, @PathVariable String docId) {
        long deleted = ingestService.deleteDocument(kbId, docId);
        return new DocumentIngestResponse(docId, 0, deleted);
    }

    private static DocumentIngestResponse toResponse(IngestResult result) {
        return new DocumentIngestResponse(result.docId(), result.chunkCount(), result.deletedChunks());
    }
}
