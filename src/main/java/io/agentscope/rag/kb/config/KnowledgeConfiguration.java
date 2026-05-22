package io.agentscope.rag.kb.config;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.VDBStoreBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        prefix = "agentscope.rag.simple",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class KnowledgeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeConfiguration.class);

    @Bean
    public EmbeddingModel kbEmbeddingModel(SimpleRagProperties properties) {
        SimpleRagProperties.EmbeddingProperties embedding = properties.getEmbedding();
        log.info(
                "Creating DashScope embedding: model={}, dimensions={}",
                embedding.getModelName(),
                embedding.getDimensions());
        return DashScopeTextEmbedding.builder()
                .apiKey(embedding.getApiKey())
                .modelName(embedding.getModelName())
                .dimensions(embedding.getDimensions())
                .build();
    }

    @Bean
    public SimpleKnowledge kbSimpleKnowledge(EmbeddingModel kbEmbeddingModel, VDBStoreBase kbVectorStore) {
        return SimpleKnowledge.builder()
                .embeddingModel(kbEmbeddingModel)
                .embeddingStore(kbVectorStore)
                .build();
    }

    @Bean
    public Knowledge kbKnowledge(SimpleKnowledge kbSimpleKnowledge) {
        return kbSimpleKnowledge;
    }

    @Bean
    public TextReader kbTextReader(SimpleRagProperties properties) {
        SimpleRagProperties.ReaderProperties reader = properties.getReader();
        SplitStrategy strategy =
                reader.getSplitStrategy() != null ? reader.getSplitStrategy() : SplitStrategy.PARAGRAPH;
        return new TextReader(reader.getChunkSize(), strategy, reader.getChunkOverlap());
    }

    @Bean
    public RetrieveConfig kbDefaultRetrieveConfig(SimpleRagProperties properties) {
        SimpleRagProperties.RetrieveProperties retrieve = properties.getRetrieve();
        return RetrieveConfig.builder()
                .limit(retrieve.getLimit())
                .scoreThreshold(retrieve.getScoreThreshold())
                .build();
    }
}
