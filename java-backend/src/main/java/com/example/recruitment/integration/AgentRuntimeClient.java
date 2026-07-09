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
public class AgentRuntimeClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AgentRuntimeClient(
            @Value("${services.agent-runtime-base-url:http://localhost:8100}") String baseUrl,
            ObjectMapper objectMapper) {
        this(RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(baseUrl)
                .build(), objectMapper);
    }

    AgentRuntimeClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public ResumeParseResponse parseResume(ResumeParseRequest request) {
        return post("/internal/agents/resume-parse", request, ResumeParseResponse.class);
    }

    public ResumeAnalysisResponse analyzeResume(ResumeAnalysisRequest request) {
        return post("/internal/agents/resume-analysis", request, ResumeAnalysisResponse.class);
    }

    public EmailDraftResponse draftEmail(EmailDraftRequest request) {
        return post("/internal/agents/email-draft", request, EmailDraftResponse.class);
    }

    public InterviewPlanResponse planInterview(InterviewPlanRequest request) {
        return post("/internal/agents/interview-plan", request, InterviewPlanResponse.class);
    }

    public QuestionAnswerResponse answerQuestion(QuestionAnswerRequest request) {
        return post("/internal/agents/qa", request, QuestionAnswerResponse.class);
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
            throw new IllegalArgumentException("Failed to serialize agent runtime request", ex);
        }
    }

    public record ResumeParseRequest(String traceId, String fileName, String fileContentBase64) {
    }

    public record ResumeParseResponse(String parsedText, String language, String parseStatus, String errorMessage) {
    }

    public record CandidatePayload(String id, String name, String email) {
    }

    public record JobPayload(
            String id,
            String title,
            List<String> requiredSkills,
            List<String> preferredSkills,
            String experienceRequirement,
            String description,
            String interviewRequirements) {
    }

    public record ResumePayload(String id, String parsedText) {
    }

    public record RagEvidence(String evidenceId, String sourceType, String content, double score) {
    }

    public record ResumeAnalysisRequest(
            String traceId,
            String applicationId,
            CandidatePayload candidate,
            JobPayload job,
            ResumePayload resume,
            List<RagEvidence> ragEvidence) {
    }

    public record TokenUsage(int inputTokens, int outputTokens, int totalTokens) {
    }

    public record ResumeAnalysisResponse(
            String agentRunId,
            int overallScore,
            String recommendation,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> strengths,
            List<String> risks,
            String summary,
            boolean requiresHumanReview,
            String modelName,
            TokenUsage tokenUsage) {
    }

    public record EmailDraftRequest(
            String traceId,
            String applicationId,
            String emailType,
            CandidatePayload candidate,
            JobPayload job,
            Map<String, Object> analysisReport) {
    }

    public record EmailDraftResponse(
            String agentRunId,
            String subject,
            String body,
            boolean requiresHumanReview,
            String modelName,
            TokenUsage tokenUsage) {
    }

    public record TimeSlot(String startTime, String endTime) {
    }

    public record InterviewPlanRequest(
            String traceId,
            String applicationId,
            String interviewType,
            String preferredTimezone,
            List<TimeSlot> candidateAvailableSlots,
            JobPayload job,
            Map<String, Object> analysisReport) {
    }

    public record InterviewPlanResponse(
            String agentRunId,
            String proposedStartTime,
            String proposedEndTime,
            String timezone,
            String meetingTitle,
            String meetingNotes,
            boolean requiresHumanReview,
            String modelName,
            TokenUsage tokenUsage) {
    }

    public record QuestionAnswerRequest(String traceId, String question, List<RagEvidence> context) {
    }

    public record QuestionAnswerResponse(String answer, List<String> evidenceIds, String modelName, TokenUsage tokenUsage) {
    }
}
