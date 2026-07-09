package com.example.recruitment.integration;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RagServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public RagServiceClient(
            @Value("${services.rag-service-base-url:http://localhost:8200}") String baseUrl,
            ObjectMapper objectMapper) {
        this(RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(baseUrl)
                .build(), objectMapper);
    }

    RagServiceClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public RagDocumentResponse indexDocument(RagDocumentRequest request) {
        return post("/internal/rag/documents", request, RagDocumentResponse.class);
    }

    public RagSearchResponse search(RagSearchRequest request) {
        return post("/internal/rag/search", request, RagSearchResponse.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(toJson(body))
                .retrieve()
                .body(responseType);
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize RAG service request", ex);
        }
    }

    public record RagDocumentRequest(
            String traceId,
            String sourceType,
            String sourceId,
            String applicationId,
            String jobPositionId,
            String content,
            Map<String, Object> metadata) {
    }

    public record RagDocumentResponse(String documentId, int chunkCount, String status) {
    }

    public record RagSearchRequest(
            String traceId,
            String query,
            List<String> sourceTypes,
            int topK,
            Map<String, String> filters) {
    }

    public record RagSearchItem(
            String evidenceId,
            String sourceType,
            String sourceId,
            String chunkId,
            String content,
            double score,
            Map<String, Object> metadata) {
    }

    public record RagSearchResponse(List<RagSearchItem> items) {
    }
}
