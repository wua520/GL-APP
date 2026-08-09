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
import java.util.List;

@Component
public class OllamaEmbeddingClient implements EmbeddingClient {
    private final KnowledgeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaEmbeddingClient(KnowledgeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getRetrieval().getConnectTimeoutMs()))
            .build();
    }

    @Override
    public List<Double> embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                "model", properties.getOllama().getEmbeddingModel(), "prompt", text
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create(normalizeBaseUrl() + "/api/embeddings"))
                .timeout(Duration.ofMillis(properties.getRetrieval().getReadTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama embedding request failed: " + response.statusCode());
            }
            JsonNode embedding = objectMapper.readTree(response.body()).path("embedding");
            if (!embedding.isArray() || embedding.isEmpty()) {
                throw new IllegalStateException("Ollama embedding response is empty");
            }
            List<Double> result = new ArrayList<>();
            for (JsonNode item : embedding) result.add(item.asDouble());
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Ollama embedding unavailable", exception);
        }
    }

    private String normalizeBaseUrl() {
        return properties.getOllama().getBaseUrl().replaceAll("/+$", "");
    }
}
