package io.agentscope.rag.kb.config;

import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.store.ElasticsearchStore;
import io.agentscope.core.rag.store.InMemoryStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "agentscope.rag.simple", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StoreConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StoreConfiguration.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "agentscope.rag.simple",
            name = "store-type",
            havingValue = "elasticsearch",
            matchIfMissing = true)
    public ElasticsearchStore elasticsearchStore(SimpleRagProperties properties) throws VectorStoreException {
        SimpleRagProperties.ElasticsearchProperties es = properties.getElasticsearch();
        int dimensions = properties.getEmbedding().getDimensions();
        log.info(
                "Creating ElasticsearchStore url={} index={} dimensions={}",
                es.getUrl(),
                es.getIndexName(),
                dimensions);

        ElasticsearchStore.Builder builder =
                ElasticsearchStore.builder()
                        .url(es.getUrl())
                        .indexName(es.getIndexName())
                        .dimensions(dimensions);

        if (es.hasCredentials()) {
            builder.username(es.getUsername()).password(es.getPassword());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agentscope.rag.simple", name = "store-type", havingValue = "memory")
    public VDBStoreBase inMemoryStore(SimpleRagProperties properties) {
        int dimensions = properties.getEmbedding().getDimensions();
        log.info("Creating InMemoryStore dimensions={}", dimensions);
        return InMemoryStore.builder().dimensions(dimensions).build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "agentscope.rag.simple",
            name = "store-type",
            havingValue = "elasticsearch",
            matchIfMissing = true)
    public VDBStoreBase kbVectorStore(ElasticsearchStore elasticsearchStore) {
        return elasticsearchStore;
    }
}
