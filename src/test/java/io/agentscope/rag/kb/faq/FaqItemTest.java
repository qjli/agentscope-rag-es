package io.agentscope.rag.kb.faq;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FaqItemTest {

    @Test
    void toKnowledgeTextContainsQuestion() {
        FaqItem item = new FaqItem();
        item.setId("faq-001");
        item.setCategory("账号");
        item.setQuestion("如何重置密码？");
        item.setAnswer("点击忘记密码。");
        assertTrue(item.toKnowledgeText().contains("如何重置密码"));
    }
}
