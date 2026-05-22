package io.agentscope.rag.kb.ops;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.exception.VectorStoreException;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.store.ElasticsearchStore;
import io.agentscope.core.rag.store.VDBStoreBase;
import io.agentscope.rag.kb.config.OpsProperties;
import io.agentscope.rag.kb.config.SimpleRagProperties;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseRegistry {

    public static final String DEFAULT_KB_ID = "default";

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseRegistry.class);

    private final SimpleRagProperties properties;
    private final OpsProperties opsProperties;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final Optional<VDBStoreBase> defaultVectorStore;
    private final Optional<ElasticsearchDocMaintenance> defaultMaintenance;

    private final Map<String, KnowledgeBaseDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, RuntimeKnowledgeBase> runtimes = new ConcurrentHashMap<>();

    public KnowledgeBaseRegistry(
            SimpleRagProperties properties,
            OpsProperties opsProperties,
            EmbeddingModel embeddingModel,
            ObjectMapper objectMapper,
            @Autowired(required = false) VDBStoreBase kbVectorStore,
            @Autowired(required = false) ElasticsearchDocMaintenance esMaintenance) {
        this.properties = properties;
        this.opsProperties = opsProperties;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
        this.defaultVectorStore = Optional.ofNullable(kbVectorStore);
        this.defaultMaintenance = Optional.ofNullable(esMaintenance);
    }

    @PostConstruct
    void init() throws IOException {
        Path dataDir = Path.of(opsProperties.getDataDir());
        Files.createDirectories(dataDir);
        Files.createDirectories(Path.of(opsProperties.getUploadDir()));

        loadPersistedDescriptors();

        SimpleRagProperties.ElasticsearchProperties es = properties.getElasticsearch();
        KnowledgeBaseDescriptor builtIn =
                new KnowledgeBaseDescriptor(
                        DEFAULT_KB_ID,
                        "默认知识库",
                        es.getIndexName(),
                        "来自 application.yml 的默认 ES 索引",
                        Instant.now(),
                        true);
        descriptors.put(DEFAULT_KB_ID, builtIn);
        for (KnowledgeBaseDescriptor descriptor : List.copyOf(descriptors.values())) {
            if (properties.getStoreType() != SimpleRagProperties.StoreType.ELASTICSEARCH
                    && !DEFAULT_KB_ID.equals(descriptor.getId())) {
                continue;
            }
            ensureRuntime(descriptor);
        }
        persistDescriptors();
    }

    @PreDestroy
    void shutdown() {
        for (RuntimeKnowledgeBase runtime : runtimes.values()) {
            closeQuietly(runtime);
        }
        runtimes.clear();
    }

    public List<KnowledgeBaseDescriptor> listDescriptors() {
        return new ArrayList<>(descriptors.values());
    }

    public KnowledgeBaseContext require(String kbId) {
        KnowledgeBaseDescriptor descriptor =
                descriptors.get(kbId != null ? kbId.trim() : null);
        if (descriptor == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + kbId);
        }
        RuntimeKnowledgeBase runtime = ensureRuntime(descriptor);
        return new KnowledgeBaseContext(descriptor, runtime.knowledge(), runtime.maintenance());
    }

    public KnowledgeBaseContext requireDefault() {
        return require(DEFAULT_KB_ID);
    }

    public KnowledgeBaseDescriptor create(String id, String displayName, String indexName, String description) {
        String normalizedId = normalizeId(id);
        if (descriptors.containsKey(normalizedId)) {
            throw new IllegalArgumentException("Knowledge base already exists: " + normalizedId);
        }
        String normalizedIndex = normalizeIndexName(indexName);
        if (descriptors.values().stream().anyMatch(d -> d.getIndexName().equals(normalizedIndex))) {
            throw new IllegalArgumentException("ES index name already used: " + normalizedIndex);
        }
        if (properties.getStoreType() != SimpleRagProperties.StoreType.ELASTICSEARCH) {
            throw new IllegalStateException("Multiple knowledge bases require store-type=elasticsearch");
        }

        KnowledgeBaseDescriptor descriptor =
                new KnowledgeBaseDescriptor(
                        normalizedId,
                        displayName != null && !displayName.isBlank() ? displayName.trim() : normalizedId,
                        normalizedIndex,
                        description,
                        Instant.now(),
                        false);
        descriptors.put(normalizedId, descriptor);
        ensureRuntime(descriptor);
        persistDescriptors();
        log.info("Registered knowledge base id={} index={}", normalizedId, normalizedIndex);
        return descriptor;
    }

    private void loadPersistedDescriptors() throws IOException {
        Path file = Path.of(opsProperties.getRegistryFile());
        if (!Files.exists(file)) {
            return;
        }
        List<KnowledgeBaseDescriptor> loaded =
                objectMapper.readValue(file.toFile(), new TypeReference<List<KnowledgeBaseDescriptor>>() {});
        for (KnowledgeBaseDescriptor descriptor : loaded) {
            if (descriptor.getId() == null || descriptor.getId().isBlank()) {
                continue;
            }
            if (DEFAULT_KB_ID.equals(descriptor.getId())) {
                continue;
            }
            if (properties.getStoreType() == SimpleRagProperties.StoreType.ELASTICSEARCH) {
                descriptors.put(descriptor.getId(), descriptor);
            }
        }
    }

    private void persistDescriptors() {
        try {
            Path file = Path.of(opsProperties.getRegistryFile());
            Files.createDirectories(file.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), listDescriptors());
        } catch (IOException e) {
            log.warn("Failed to persist knowledge base registry: {}", e.getMessage());
        }
    }

    private RuntimeKnowledgeBase ensureRuntime(KnowledgeBaseDescriptor descriptor) {
        return runtimes.computeIfAbsent(descriptor.getId(), id -> createRuntime(descriptor));
    }

    private RuntimeKnowledgeBase createRuntime(KnowledgeBaseDescriptor descriptor) {
        if (DEFAULT_KB_ID.equals(descriptor.getId()) && defaultVectorStore.isPresent()) {
            SimpleKnowledge knowledge =
                    SimpleKnowledge.builder()
                            .embeddingModel(embeddingModel)
                            .embeddingStore(defaultVectorStore.get())
                            .build();
            return new RuntimeKnowledgeBase(knowledge, defaultMaintenance, null);
        }

        if (properties.getStoreType() != SimpleRagProperties.StoreType.ELASTICSEARCH) {
            throw new IllegalStateException(
                    "Only the default knowledge base is available when store-type=memory");
        }

        try {
            SimpleRagProperties.ElasticsearchProperties es = properties.getElasticsearch();
            ElasticsearchStore store =
                    ElasticsearchStore.builder()
                            .url(es.getUrl())
                            .indexName(descriptor.getIndexName())
                            .dimensions(properties.getEmbedding().getDimensions())
                            .username(es.hasCredentials() ? es.getUsername() : null)
                            .password(es.hasCredentials() ? es.getPassword() : null)
                            .build();
            SimpleKnowledge knowledge =
                    SimpleKnowledge.builder().embeddingModel(embeddingModel).embeddingStore(store).build();
            ElasticsearchDocMaintenance maintenance =
                    new ElasticsearchDocMaintenance(properties, objectMapper, descriptor.getIndexName());
            return new RuntimeKnowledgeBase(knowledge, Optional.of(maintenance), store);
        } catch (VectorStoreException e) {
            throw new IllegalStateException("Failed to create ES store for " + descriptor.getId(), e);
        }
    }

    private static void closeQuietly(RuntimeKnowledgeBase runtime) {
        if (runtime.elasticsearchStore() != null) {
            try {
                runtime.elasticsearchStore().close();
            } catch (Exception e) {
                log.warn("Failed to close ElasticsearchStore: {}", e.getMessage());
            }
        }
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Knowledge base id is required");
        }
        String normalized = id.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Invalid knowledge base id");
        }
        return normalized;
    }

    private static String normalizeIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException("indexName is required");
        }
        String normalized = indexName.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Invalid index name");
        }
        return normalized;
    }

    private record RuntimeKnowledgeBase(
            Knowledge knowledge,
            Optional<ElasticsearchDocMaintenance> maintenance,
            ElasticsearchStore elasticsearchStore) {}
}
