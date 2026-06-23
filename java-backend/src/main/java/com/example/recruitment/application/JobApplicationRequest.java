package com.example.recruitment.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class JobApplicationRequest {

    @NotNull
    private Long jobId;

    @NotNull
    private Long candidateId;

    @NotNull
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Min(0)
    @Max(100)
    private Integer resumeScore;

    private String screeningSummary;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Integer getResumeScore() {
        return resumeScore;
    }

    public void setResumeScore(Integer resumeScore) {
        this.resumeScore = resumeScore;
    }

    public String getScreeningSummary() {
        return screeningSummary;
    }

    public void setScreeningSummary(String screeningSummary) {
        this.screeningSummary = screeningSummary;
    }
}
