package io.agentscope.rag.kb.health;

import io.agentscope.rag.kb.config.SimpleRagProperties;
import io.agentscope.rag.kb.faq.KbIndexRegistry;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class KbElasticsearchHealthIndicator implements HealthIndicator {

    private final SimpleRagProperties properties;
    private final KbIndexRegistry registry;
    private final ElasticsearchDocMaintenance esMaintenance;

    public KbElasticsearchHealthIndicator(
            SimpleRagProperties properties,
            KbIndexRegistry registry,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.properties = properties;
        this.registry = registry;
        this.esMaintenance = esMaintenance;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.outOfService().withDetail("reason", "RAG disabled").build();
        }

        if (properties.getStoreType() != SimpleRagProperties.StoreType.ELASTICSEARCH) {
            return Health.up()
                    .withDetail("store", "InMemoryStore")
                    .withDetail("chunks", registry.getChunkCount())
                    .build();
        }

        if (esMaintenance == null) {
            return Health.down().withDetail("reason", "Elasticsearch maintenance not configured").build();
        }

        if (registry.getLastError() != null) {
            return Health.down().withDetail("lastError", registry.getLastError()).build();
        }

        boolean ping = esMaintenance.ping();
        long count = esMaintenance.countDocuments();

        if (!ping) {
            return Health.down()
                    .withDetail("reason", "Elasticsearch ping failed")
                    .withDetail("url", properties.getElasticsearch().getUrl())
                    .build();
        }

        Health.Builder builder =
                Health.up()
                        .withDetail("store", "ElasticsearchStore")
                        .withDetail("index", esMaintenance.getIndexName())
                        .withDetail("documentCount", count);

        if (count <= 0) {
            builder.withDetail(
                    "hint", "Ingest via POST /api/v1/kb/documents or POST /api/v1/faq/reload");
        }
        return builder.build();
    }
}
