package io.agentscope.rag.kb.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ConditionalOnProperty(prefix = "agentscope.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReActAgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentConfiguration.class);

    @Bean
    public Toolkit kbAgentToolkit() {
        return new Toolkit();
    }

    @Bean
    @DependsOn("kbKnowledge")
    public ReActAgent kbAssistantAgent(
            AgentProperties agentProperties, Knowledge kbKnowledge, Toolkit kbAgentToolkit) {

        String apiKey = agentProperties.getDashscopeApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "agentscope.agent.dashscope-api-key is required when agentscope.agent.enabled=true");
        }

        AgentProperties.RetrieveProperties retrieve = agentProperties.getRetrieve();
        RetrieveConfig retrieveConfig =
                RetrieveConfig.builder()
                        .limit(retrieve.getLimit())
                        .scoreThreshold(retrieve.getScoreThreshold())
                        .build();

        RAGMode ragMode = agentProperties.getRagMode() != null ? agentProperties.getRagMode() : RAGMode.GENERIC;

        ReActAgent agent =
                ReActAgent.builder()
                        .name("KbAssistant")
                        .sysPrompt(agentProperties.getSystemPrompt())
                        .model(
                                DashScopeChatModel.builder()
                                        .apiKey(apiKey)
                                        .modelName(agentProperties.getModelName())
                                        .build())
                        .toolkit(kbAgentToolkit)
                        .memory(new InMemoryMemory())
                        .knowledge(kbKnowledge)
                        .ragMode(ragMode)
                        .retrieveConfig(retrieveConfig)
                        .build();

        log.info("ReActAgent created: ragMode={}, model={}", ragMode, agentProperties.getModelName());
        return agent;
    }
}
