package com.fitness.server.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QdrantVectorStore implements VectorStore {
    private final KnowledgeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public QdrantVectorStore(KnowledgeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getRetrieval().getConnectTimeoutMs()))
            .build();
    }

    @Override
    public List<VectorHit> search(List<Double> vector, int limit) {
        try {
            try {
                return parseHits(send("/collections/" + properties.getQdrant().getCollection() + "/points/search", Map.of(
                    "vector", vector, "limit", limit, "with_payload", true
                )));
            } catch (IllegalStateException exception) {
                if (!exception.getMessage().contains("404")) throw exception;
                return parseHits(send("/collections/" + properties.getQdrant().getCollection() + "/points/query", Map.of(
                    "query", vector, "limit", limit, "with_payload", true
                )));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Qdrant search unavailable", exception);
        }
    }

    private List<VectorHit> parseHits(HttpResponse<String> response) throws Exception {
        JsonNode result = objectMapper.readTree(response.body()).path("result");
        JsonNode results = result.isArray() ? result : result.path("points");
        List<VectorHit> hits = new ArrayList<>();
        for (JsonNode item : results) {
            JsonNode chunkId = item.path("payload").path("chunkId");
            if (chunkId.canConvertToLong()) hits.add(new VectorHit(chunkId.asLong(), item.path("score").asDouble()));
        }
        return hits;
    }

    @Override
    public void upsert(long pointId, long chunkId, List<Double> vector) {
        try {
            Map<String, Object> point = Map.of("id", pointId, "vector", vector, "payload", Map.of("chunkId", chunkId));
            put("/collections/" + properties.getQdrant().getCollection() + "/points?wait=true", Map.of("points", List.of(point)));
        } catch (Exception exception) {
            throw new IllegalStateException("Qdrant upsert unavailable", exception);
        }
    }

    @Override
    public void recreateCollection(int vectorDimension) {
        if (vectorDimension <= 0) {
            throw new IllegalArgumentException("Vector dimension must be positive");
        }
        try {
            String collectionPath = "/collections/" + properties.getQdrant().getCollection();
            delete(collectionPath);
            put(collectionPath, Map.of("vectors", Map.of("size", vectorDimension, "distance", "Cosine")));
        } catch (Exception exception) {
            throw new IllegalStateException("Qdrant collection initialization unavailable", exception);
        }
    }

    private HttpResponse<String> send(String path, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl() + path))
            .timeout(Duration.ofMillis(properties.getRetrieval().getReadTimeoutMs()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();
        return requireSuccessfulResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private void delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl() + path))
            .timeout(Duration.ofMillis(properties.getRetrieval().getReadTimeoutMs()))
            .DELETE()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 404) requireSuccessfulResponse(response);
    }

    private void put(String path, Object body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl() + path))
            .timeout(Duration.ofMillis(properties.getRetrieval().getReadTimeoutMs()))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();
        requireSuccessfulResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private HttpResponse<String> requireSuccessfulResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Qdrant request failed: " + response.statusCode());
        }
        return response;
    }

    private String normalizeBaseUrl() {
        return properties.getQdrant().getBaseUrl().replaceAll("/+$", "");
    }
}
