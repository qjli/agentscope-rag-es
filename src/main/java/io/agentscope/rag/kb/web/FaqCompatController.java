package io.agentscope.rag.kb.web;

import io.agentscope.rag.kb.service.KbAdminService;
import io.agentscope.rag.kb.web.dto.KbIndexStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 兼容 02-simple-kg-code 的 FAQ 路径，内部走统一 IngestService → ES。 */
@RestController
@RequestMapping("/api/v1/faq")
@Tag(name = "FAQ Compat", description = "FAQ 热加载（写入 ES，按 faq-id 为 doc_id）")
public class FaqCompatController {

    private final KbAdminService kbAdminService;

    public FaqCompatController(KbAdminService kbAdminService) {
        this.kbAdminService = kbAdminService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "FAQ/知识库状态")
    public KbIndexStatusResponse status() {
        return kbAdminService.status();
    }

    @PostMapping(value = "/reload", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "重新加载 FAQ", description = "按 doc_id 删除旧 chunk 后重新入库到 ES")
    public KbIndexStatusResponse reload() {
        return kbAdminService.reloadFaq();
    }
}
