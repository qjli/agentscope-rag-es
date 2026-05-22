package io.agentscope.rag.kb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
        properties = {
            "agentscope.rag.simple.store-type=memory",
            "agentscope.rag.simple.embedding.api-key=sk-test-key",
            "agentscope.rag.simple.faq.bootstrap-on-startup=false",
            "agentscope.agent.enabled=false",
            "agentscope.agent.dashscope-api-key=sk-test"
        })
class SimpleKbApplicationTests {

    @Test
    void contextLoads() {}
}
