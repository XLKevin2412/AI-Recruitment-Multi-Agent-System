package com.example.recruitment.application;

public record AnalysisTriggerResponse(String applicationId, ApplicationStatus status, String agentRunId, String reportId) {
}
