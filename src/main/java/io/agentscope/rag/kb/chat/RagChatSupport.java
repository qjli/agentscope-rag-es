package io.agentscope.rag.kb.chat;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.rag.kb.config.AgentProperties;
import io.agentscope.rag.kb.web.dto.DocumentDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RagChatSupport {

    private final AgentProperties agentProperties;

    public RagChatSupport(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public RetrieveConfig agentRetrieveConfig() {
        AgentProperties.RetrieveProperties retrieve = agentProperties.getRetrieve();
        return RetrieveConfig.builder()
                .limit(retrieve.getLimit())
                .scoreThreshold(retrieve.getScoreThreshold())
                .build();
    }

    public List<DocumentDto> retrieveForChat(Knowledge knowledge, String query) {
        String trimmed = query != null ? query.trim() : "";
        if (trimmed.isEmpty()) {
            return List.of();
        }
        List<Document> documents =
                knowledge.retrieve(trimmed, agentRetrieveConfig()).blockOptional().orElse(List.of());
        return documents.stream().map(DocumentDto::from).toList();
    }
}
