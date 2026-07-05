package com.example.recruitment.template;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_templates")
public class JobTemplate {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "job_type", nullable = false, length = 80)
    private String jobType;

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

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getJobType() {
        return jobType;
    }

    public String getRequiredSkillsJson() {
        return requiredSkillsJson;
    }

    public String getPreferredSkillsJson() {
        return preferredSkillsJson;
    }

    public String getExperienceRequirement() {
        return experienceRequirement;
    }

    public String getDescription() {
        return description;
    }

    public String getInterviewRequirements() {
        return interviewRequirements;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
