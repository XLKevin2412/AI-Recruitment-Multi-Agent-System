package com.example.recruitment.integration;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RagServiceClient {

    private final RestClient restClient;

    public RagServiceClient(@Value("${services.rag-service-base-url:http://localhost:8200}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
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
                .body(body)
                .retrieve()
                .body(responseType);
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
