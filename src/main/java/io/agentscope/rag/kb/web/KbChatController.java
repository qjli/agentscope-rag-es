package io.agentscope.rag.kb.web;

import io.agentscope.rag.kb.chat.KbChatService;
import io.agentscope.rag.kb.web.dto.ChatRequest;
import io.agentscope.rag.kb.web.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kb")
@Tag(name = "Knowledge Chat", description = "ReActAgent + SimpleKnowledge RAG 对话")
public class KbChatController {

    private final KbChatService kbChatService;

    public KbChatController(KbChatService kbChatService) {
        this.kbChatService = kbChatService;
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "RAG 对话")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return kbChatService.chat(request);
    }

    @GetMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "对话接口说明")
    public ResponseEntity<Map<String, String>> chatUsage() {
        return ResponseEntity.status(405)
                .body(
                        Map.of(
                                "error",
                                "Method Not Allowed",
                                "hint",
                                "Use POST /api/v1/kb/chat with JSON: {\"message\":\"...\"}",
                                "contentType",
                                "application/json"));
    }
}
