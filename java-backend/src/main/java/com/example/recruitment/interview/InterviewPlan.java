package com.example.recruitment.interview;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "interview_plans")
public class InterviewPlan {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "application_id", nullable = false, length = 36)
    private String applicationId;

    @Column(name = "agent_run_id", length = 36)
    private String agentRunId;

    @Column(name = "interview_type", nullable = false, length = 50)
    private String interviewType;

    @Column(name = "proposed_start_time", nullable = false)
    private Instant proposedStartTime;

    @Column(name = "proposed_end_time", nullable = false)
    private Instant proposedEndTime;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "meeting_title", nullable = false, length = 255)
    private String meetingTitle;

    @Column(name = "meeting_notes", columnDefinition = "TEXT")
    private String meetingNotes;

    @Column(nullable = false, length = 30)
    private String status;

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

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public Instant getProposedStartTime() {
        return proposedStartTime;
    }

    public void setProposedStartTime(Instant proposedStartTime) {
        this.proposedStartTime = proposedStartTime;
    }

    public Instant getProposedEndTime() {
        return proposedEndTime;
    }

    public void setProposedEndTime(Instant proposedEndTime) {
        this.proposedEndTime = proposedEndTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getMeetingTitle() {
        return meetingTitle;
    }

    public void setMeetingTitle(String meetingTitle) {
        this.meetingTitle = meetingTitle;
    }

    public String getMeetingNotes() {
        return meetingNotes;
    }

    public void setMeetingNotes(String meetingNotes) {
        this.meetingNotes = meetingNotes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
