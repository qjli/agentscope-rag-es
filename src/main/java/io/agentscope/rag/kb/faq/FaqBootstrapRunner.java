package io.agentscope.rag.kb.faq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentscope.rag.simple.faq", name = "bootstrap-on-startup", havingValue = "true")
public class FaqBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FaqBootstrapRunner.class);

    private final FaqBootstrapAdapter faqBootstrapAdapter;

    public FaqBootstrapRunner(FaqBootstrapAdapter faqBootstrapAdapter) {
        this.faqBootstrapAdapter = faqBootstrapAdapter;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("FAQ bootstrap on startup enabled");
        faqBootstrapAdapter.loadAll();
    }
}
