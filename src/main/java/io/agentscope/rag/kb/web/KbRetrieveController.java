package io.agentscope.rag.kb.web;

import io.agentscope.rag.kb.retrieve.KbRetrieveService;
import io.agentscope.rag.kb.web.dto.RetrieveRequest;
import io.agentscope.rag.kb.web.dto.RetrieveResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kb")
@Tag(name = "Knowledge Retrieve", description = "向量检索（ES kNN）")
public class KbRetrieveController {

    private final KbRetrieveService kbRetrieveService;

    public KbRetrieveController(KbRetrieveService kbRetrieveService) {
        this.kbRetrieveService = kbRetrieveService;
    }

    @PostMapping(value = "/retrieve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "知识库向量检索")
    public RetrieveResponse retrieve(@Valid @RequestBody RetrieveRequest request) {
        return new RetrieveResponse(request.getQuery(), kbRetrieveService.retrieve(request));
    }
}
