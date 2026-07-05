package com.example.recruitment.job;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "job_positions")
public class Job {

    @Id
    @Column(length = 36)
    private String id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(name = "job_type", nullable = false, length = 80)
    private String jobType;

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String level;

    @NotBlank
    @Column(name = "required_skills_json", nullable = false, columnDefinition = "json")
    private String requiredSkillsJson;

    @Column(name = "preferred_skills_json", columnDefinition = "json")
    private String preferredSkillsJson;

    @Column(name = "experience_requirement", columnDefinition = "TEXT")
    private String experienceRequirement;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "interview_requirements", columnDefinition = "TEXT")
    private String interviewRequirements;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRequiredSkillsJson() {
        return requiredSkillsJson;
    }

    public void setRequiredSkillsJson(String requiredSkillsJson) {
        this.requiredSkillsJson = requiredSkillsJson;
    }

    public String getPreferredSkillsJson() {
        return preferredSkillsJson;
    }

    public void setPreferredSkillsJson(String preferredSkillsJson) {
        this.preferredSkillsJson = preferredSkillsJson;
    }

    public String getExperienceRequirement() {
        return experienceRequirement;
    }

    public void setExperienceRequirement(String experienceRequirement) {
        this.experienceRequirement = experienceRequirement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInterviewRequirements() {
        return interviewRequirements;
    }

    public void setInterviewRequirements(String interviewRequirements) {
        this.interviewRequirements = interviewRequirements;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
