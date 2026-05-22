package io.agentscope.rag.kb.config;

import io.agentscope.core.ReActAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AgentStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(AgentStartupLogger.class);

    @Autowired(required = false)
    private ReActAgent kbAssistantAgent;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (kbAssistantAgent != null) {
            log.info("ReActAgent bean ready: {}", kbAssistantAgent.getName());
        } else {
            log.warn("ReActAgent bean not created (agent disabled or misconfigured)");
        }
    }
}
