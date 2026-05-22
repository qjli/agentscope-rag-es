package io.agentscope.rag.kb.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.rag.kb.config.AgentProperties;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.rag.kb.web.dto.ChatRequest;
import io.agentscope.rag.kb.web.dto.ChatResponse;
import io.agentscope.rag.kb.web.dto.DocumentDto;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KbChatService {

    private final ReActAgent kbAssistantAgent;
    private final Knowledge kbKnowledge;
    private final RagChatSupport ragChatSupport;
    private final KbIndexRegistry registry;
    private final AgentProperties agentProperties;
    private final Optional<ElasticsearchDocMaintenance> esMaintenance;

    public KbChatService(
            @Autowired(required = false) ReActAgent kbAssistantAgent,
            Knowledge kbKnowledge,
            RagChatSupport ragChatSupport,
            KbIndexRegistry registry,
            AgentProperties agentProperties,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.kbAssistantAgent = kbAssistantAgent;
        this.kbKnowledge = kbKnowledge;
        this.ragChatSupport = ragChatSupport;
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

        String query = request.getMessage().trim();
        List<DocumentDto> retrieved = ragChatSupport.retrieveForChat(kbKnowledge, query);

        Msg userMsg =
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(query).build())
                        .build();

        Msg reply = kbAssistantAgent.call(userMsg).block();
        String text = reply != null ? reply.getTextContent() : "";
        return new ChatResponse(query, text, request.getSessionId(), null, null, retrieved);
    }
}
