package com.example.recruitment.application;

import jakarta.validation.constraints.NotNull;

public class JobApplicationUpdateRequest {

    @NotNull
    private ApplicationStatus status;

    private String resumeId;

    private String currentStage;

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }
}
