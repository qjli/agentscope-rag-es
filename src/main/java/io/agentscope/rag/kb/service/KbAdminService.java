package io.agentscope.rag.kb.service;

import io.agentscope.rag.kb.config.SimpleRagProperties;
import io.agentscope.rag.kb.faq.FaqBootstrapAdapter;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import io.agentscope.rag.kb.web.dto.KbIndexStatusResponse;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KbAdminService {

    private final FaqBootstrapAdapter faqBootstrapAdapter;
    private final KbIndexRegistry registry;
    private final SimpleRagProperties properties;
    private final Optional<ElasticsearchDocMaintenance> esMaintenance;

    public KbAdminService(
            FaqBootstrapAdapter faqBootstrapAdapter,
            KbIndexRegistry registry,
            SimpleRagProperties properties,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.faqBootstrapAdapter = faqBootstrapAdapter;
        this.registry = registry;
        this.properties = properties;
        this.esMaintenance = Optional.ofNullable(esMaintenance);
    }

    public KbIndexStatusResponse status() {
        return buildStatus(null, null, null);
    }

    public KbIndexStatusResponse reloadFaq() {
        if (!properties.getFaq().isReloadEnabled()) {
            throw new IllegalStateException("FAQ reload is disabled by configuration");
        }
        FaqBootstrapAdapter.LoadResult result = faqBootstrapAdapter.loadAll();
        return buildStatus(result.itemCount(), result.chunkCount(), result.location());
    }

    private KbIndexStatusResponse buildStatus(Integer faqItems, Integer chunks, String location) {
        long esChunkCount = esMaintenance.map(ElasticsearchDocMaintenance::countDocuments).orElse(-1L);
        boolean esPing = esMaintenance.map(ElasticsearchDocMaintenance::ping).orElse(false);
        String indexName =
                esMaintenance.map(ElasticsearchDocMaintenance::getIndexName).orElse("n/a");

        boolean ready = esChunkCount > 0 || registry.isReady();

        return new KbIndexStatusResponse(
                ready,
                properties.getStoreType().name(),
                indexName,
                properties.getElasticsearch().getUrl(),
                esPing,
                esChunkCount,
                faqItems != null ? faqItems : registry.getDocumentCount(),
                chunks != null ? chunks : registry.getChunkCount(),
                registry.getLastIngestAt(),
                registry.getLastError(),
                location != null ? location : properties.getFaq().getDataLocation());
    }
}
