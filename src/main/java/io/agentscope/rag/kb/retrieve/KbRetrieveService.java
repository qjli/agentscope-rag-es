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

    /** ES kNN：numCandidates = max(limit*2, 50)，须 ≤ 10000，故 limit 最大 5000；业务侧限制 100 即可。 */
    private static final int MAX_RETRIEVE_LIMIT = 100;

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

        int limit = resolveLimit(request.getLimit());

        RetrieveConfig config =
                defaultRetrieveConfig
                        .mutate()
                        .limit(limit)
                        .scoreThreshold(
                                request.getScoreThreshold() != null
                                        ? request.getScoreThreshold()
                                        : defaultRetrieveConfig.getScoreThreshold())
                        .build();

        List<Document> documents =
                kbKnowledge.retrieve(request.getQuery().trim(), config).blockOptional().orElse(List.of());

        return documents.stream().map(DocumentDto::from).toList();
    }

    private int resolveLimit(Integer requestLimit) {
        int limit =
                requestLimit != null ? requestLimit : defaultRetrieveConfig.getLimit();
        if (limit < 1) {
            throw new IllegalArgumentException("retrieve limit must be >= 1");
        }
        if (limit > MAX_RETRIEVE_LIMIT) {
            throw new IllegalArgumentException(
                    "retrieve limit must be <= "
                            + MAX_RETRIEVE_LIMIT
                            + " (Elasticsearch numCandidates = limit*2 cannot exceed 10000)");
        }
        return limit;
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
