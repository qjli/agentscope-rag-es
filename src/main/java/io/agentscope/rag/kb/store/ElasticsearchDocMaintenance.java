package io.agentscope.rag.kb.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.rag.kb.config.SimpleRagProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ES 索引运维（REST）：按 {@code doc_id} 删除、统计、连通性。
 * AgentScope {@link io.agentscope.core.rag.store.ElasticsearchStore} 未提供 deleteByDocId。
 */
@Component
@ConditionalOnProperty(
        prefix = "agentscope.rag.simple",
        name = "store-type",
        havingValue = "elasticsearch",
        matchIfMissing = true)
public class ElasticsearchDocMaintenance {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchDocMaintenance.class);

    private final String baseUrl;
    private final String indexName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authHeader;

    public ElasticsearchDocMaintenance(SimpleRagProperties properties, ObjectMapper objectMapper) {
        SimpleRagProperties.ElasticsearchProperties es = properties.getElasticsearch();
        this.baseUrl = stripTrailingSlash(es.getUrl());
        this.indexName = es.getIndexName();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        if (es.hasCredentials()) {
            String token =
                    Base64.getEncoder()
                            .encodeToString(
                                    (es.getUsername() + ":" + es.getPassword())
                                            .getBytes(StandardCharsets.UTF_8));
            this.authHeader = "Basic " + token;
        } else {
            this.authHeader = null;
        }
        log.info("ElasticsearchDocMaintenance REST client for {} index={}", baseUrl, indexName);
    }

    public boolean ping() {
        try {
            HttpResponse<String> response =
                    httpClient.send(request("/").GET().build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("ES ping failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean indexExists() {
        try {
            HttpResponse<String> response =
                    httpClient.send(
                            request("/" + indexName).GET().build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("ES index exists check failed: {}", e.getMessage());
            return false;
        }
    }

    public long countDocuments() {
        try {
            if (!indexExists()) {
                return 0;
            }
            HttpResponse<String> response =
                    httpClient.send(
                            request("/" + indexName + "/_count").GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                return -1;
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("count").asLong(-1);
        } catch (Exception e) {
            log.warn("ES count failed: {}", e.getMessage());
            return -1;
        }
    }

    public long deleteByDocId(String docId) {
        if (docId == null || docId.isBlank()) {
            throw new IllegalArgumentException("docId is required");
        }
        try {
            if (!indexExists()) {
                return 0;
            }
            String body =
                    objectMapper.writeValueAsString(
                            Map.of("query", Map.of("term", Map.of("doc_id", docId))));
            HttpRequest httpRequest =
                    request("/" + indexName + "/_delete_by_query")
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "delete_by_query failed: HTTP " + response.statusCode() + " " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            long deleted = root.path("deleted").asLong(0);
            log.debug("delete_by_query doc_id={} deleted={}", docId, deleted);
            return deleted;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete doc_id=" + docId + " from ES", e);
        }
    }

    public String getIndexName() {
        return indexName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(30));
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        return builder;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
