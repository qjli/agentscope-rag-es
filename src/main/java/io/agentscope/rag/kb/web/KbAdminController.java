package io.agentscope.rag.kb.web;

import io.agentscope.rag.kb.service.KbAdminService;
import io.agentscope.rag.kb.web.dto.KbIndexStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kb")
@Tag(name = "Knowledge Admin", description = "索引状态（ES 连通与文档数）")
public class KbAdminController {

    private final KbAdminService kbAdminService;

    public KbAdminController(KbAdminService kbAdminService) {
        this.kbAdminService = kbAdminService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "知识库与 ES 状态")
    public KbIndexStatusResponse status() {
        return kbAdminService.status();
    }
}
