package io.agentscope.rag.kb.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.rag.kb.config.AgentProperties;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import io.agentscope.rag.kb.web.dto.ChatRequest;
import io.agentscope.rag.kb.web.dto.ChatResponse;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KbChatService {

    private final ReActAgent kbAssistantAgent;
    private final KbIndexRegistry registry;
    private final AgentProperties agentProperties;
    private final Optional<ElasticsearchDocMaintenance> esMaintenance;

    public KbChatService(
            @Autowired(required = false) ReActAgent kbAssistantAgent,
            KbIndexRegistry registry,
            AgentProperties agentProperties,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.kbAssistantAgent = kbAssistantAgent;
        this.registry = registry;
        this.agentProperties = agentProperties;
        this.esMaintenance = Optional.ofNullable(esMaintenance);
    }

    public ChatResponse chat(ChatRequest request) {
        if (!agentProperties.isEnabled()) {
            throw new IllegalStateException(
                    "Agent chat is disabled. Set agentscope.agent.enabled=true and configure"
                            + " agentscope.agent.dashscope-api-key.");
        }
        if (kbAssistantAgent == null) {
            throw new IllegalStateException(
                    "ReActAgent is not available. Check agentscope.agent.dashscope-api-key and restart.");
        }

        long esCount = esMaintenance.map(ElasticsearchDocMaintenance::countDocuments).orElse(-1L);
        if (esCount <= 0 && !registry.isReady()) {
            throw new IllegalStateException(
                    "Knowledge base is empty. Ingest documents or POST /api/v1/faq/reload first.");
        }

        Msg userMsg =
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(request.getMessage().trim()).build())
                        .build();

        Msg reply = kbAssistantAgent.call(userMsg).block();
        String text = reply != null ? reply.getTextContent() : "";
        return new ChatResponse(text, request.getSessionId());
    }
}
