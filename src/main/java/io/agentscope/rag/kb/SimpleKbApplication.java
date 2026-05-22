package io.agentscope.rag.kb;

import io.agentscope.rag.kb.config.AgentProperties;
import io.agentscope.rag.kb.config.SimpleRagProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SimpleRagProperties.class, AgentProperties.class})
public class SimpleKbApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleKbApplication.class, args);
    }
}
