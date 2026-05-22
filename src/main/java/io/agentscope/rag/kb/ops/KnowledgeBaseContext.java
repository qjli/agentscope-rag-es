package io.agentscope.rag.kb.ops;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.rag.kb.store.ElasticsearchDocMaintenance;
import java.util.Optional;

public record KnowledgeBaseContext(
        KnowledgeBaseDescriptor descriptor, Knowledge knowledge, Optional<ElasticsearchDocMaintenance> maintenance) {}
