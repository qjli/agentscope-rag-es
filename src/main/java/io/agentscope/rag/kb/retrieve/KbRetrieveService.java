package io.agentscope.rag.kb.retrieve;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import io.agentscope.rag.kb.web.dto.DocumentDto;
import io.agentscope.rag.kb.web.dto.RetrieveRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KbRetrieveService {

    private final Knowledge kbKnowledge;
    private final RetrieveConfig defaultRetrieveConfig;
    private final KbIndexRegistry registry;
    private final Optional<ElasticsearchDocMaintenance> esMaintenance;

    public KbRetrieveService(
            Knowledge kbKnowledge,
            RetrieveConfig kbDefaultRetrieveConfig,
            KbIndexRegistry registry,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.kbKnowledge = kbKnowledge;
        this.defaultRetrieveConfig = kbDefaultRetrieveConfig;
        this.registry = registry;
        this.esMaintenance = Optional.ofNullable(esMaintenance);
    }

    public List<DocumentDto> retrieve(RetrieveRequest request) {
        ensureKnowledgeAvailable();

        RetrieveConfig config =
                defaultRetrieveConfig
                        .mutate()
                        .limit(
                                request.getLimit() != null
                                        ? request.getLimit()
                                        : defaultRetrieveConfig.getLimit())
                        .scoreThreshold(
                                request.getScoreThreshold() != null
                                        ? request.getScoreThreshold()
                                        : defaultRetrieveConfig.getScoreThreshold())
                        .build();

        List<Document> documents =
                kbKnowledge.retrieve(request.getQuery().trim(), config).blockOptional().orElse(List.of());

        return documents.stream().map(DocumentDto::from).toList();
    }

    private void ensureKnowledgeAvailable() {
        long esCount = esMaintenance.map(ElasticsearchDocMaintenance::countDocuments).orElse(-1L);
        if (esCount > 0) {
            return;
        }
        if (!registry.isReady()) {
            throw new IllegalStateException(
                    "Knowledge base is empty. POST /api/v1/kb/documents or /api/v1/faq/reload with valid"
                            + " DASHSCOPE_API_KEY.");
        }
    }
}
