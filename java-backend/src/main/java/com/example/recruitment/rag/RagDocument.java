package com.example.recruitment.rag;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "rag_documents")
public class RagDocument {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 36)
    private String sourceId;

    @Column(name = "application_id", length = 36)
    private String applicationId;

    @Column(name = "job_position_id", length = 36)
    private String jobPositionId;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
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

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setJobPositionId(String jobPositionId) {
        this.jobPositionId = jobPositionId;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
