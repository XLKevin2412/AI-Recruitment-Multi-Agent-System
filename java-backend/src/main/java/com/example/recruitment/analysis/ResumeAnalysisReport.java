package com.example.recruitment.analysis;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "resume_analysis_reports")
public class ResumeAnalysisReport {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "application_id", nullable = false, length = 36)
    private String applicationId;

    @Column(name = "agent_run_id", length = 36)
    private String agentRunId;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(nullable = false, length = 50)
    private String recommendation;

    @Column(name = "matched_skills_json", columnDefinition = "json")
    private String matchedSkillsJson;

    @Column(name = "missing_skills_json", columnDefinition = "json")
    private String missingSkillsJson;

    @Column(name = "strengths_json", columnDefinition = "json")
    private String strengthsJson;

    @Column(name = "risks_json", columnDefinition = "json")
    private String risksJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "rag_evidence_ids_json", columnDefinition = "json")
    private String ragEvidenceIdsJson;

    @Column(name = "requires_human_review", nullable = false)
    private boolean requiresHumanReview;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getAgentRunId() {
        return agentRunId;
    }

    public void setAgentRunId(String agentRunId) {
        this.agentRunId = agentRunId;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getMatchedSkillsJson() {
        return matchedSkillsJson;
    }

    public void setMatchedSkillsJson(String matchedSkillsJson) {
        this.matchedSkillsJson = matchedSkillsJson;
    }

    public String getMissingSkillsJson() {
        return missingSkillsJson;
    }

    public void setMissingSkillsJson(String missingSkillsJson) {
        this.missingSkillsJson = missingSkillsJson;
    }

    public String getStrengthsJson() {
        return strengthsJson;
    }

    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    public String getRisksJson() {
        return risksJson;
    }

    public void setRisksJson(String risksJson) {
        this.risksJson = risksJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRagEvidenceIdsJson() {
        return ragEvidenceIdsJson;
    }

    public void setRagEvidenceIdsJson(String ragEvidenceIdsJson) {
        this.ragEvidenceIdsJson = ragEvidenceIdsJson;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public void setRequiresHumanReview(boolean requiresHumanReview) {
        this.requiresHumanReview = requiresHumanReview;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
