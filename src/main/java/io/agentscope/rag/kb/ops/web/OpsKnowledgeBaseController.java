package io.agentscope.rag.kb.ops.web;

import io.agentscope.rag.kb.ops.KnowledgeBaseDescriptor;
import io.agentscope.rag.kb.ops.KnowledgeBaseRegistry;
import io.agentscope.rag.kb.ops.OpsDashboardService;
import io.agentscope.rag.kb.ops.dto.CreateKnowledgeBaseRequest;
import io.agentscope.rag.kb.ops.dto.KbDashboardResponse;
import io.agentscope.rag.kb.ops.dto.KbDocumentRow;
import io.agentscope.rag.kb.ops.dto.KnowledgeBaseSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/knowledge-bases")
@Tag(name = "Ops Knowledge Bases", description = "多知识库（ES index-name）运维")
public class OpsKnowledgeBaseController {

    private final OpsDashboardService dashboardService;
    private final KnowledgeBaseRegistry registry;

    public OpsKnowledgeBaseController(OpsDashboardService dashboardService, KnowledgeBaseRegistry registry) {
        this.dashboardService = dashboardService;
        this.registry = registry;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "列出知识库")
    public List<KnowledgeBaseSummary> list() {
        return dashboardService.listKnowledgeBases();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建知识库", description = "绑定新的 ES index-name")
    public KnowledgeBaseDescriptor create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return registry.create(
                request.getId(),
                request.getDisplayName(),
                request.getIndexName(),
                request.getDescription());
    }

    @GetMapping(value = "/{kbId}/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "RAG Dashboard 指标")
    public KbDashboardResponse dashboard(@PathVariable String kbId) {
        return dashboardService.dashboard(kbId);
    }

    @GetMapping(value = "/{kbId}/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "文档列表（按 doc_id 聚合）")
    public List<KbDocumentRow> documents(
            @PathVariable String kbId, @RequestParam(defaultValue = "50") int limit) {
        return dashboardService.listDocuments(kbId, limit);
    }
}
