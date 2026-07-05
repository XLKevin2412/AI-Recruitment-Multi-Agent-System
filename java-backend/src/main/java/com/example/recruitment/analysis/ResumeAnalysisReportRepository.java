package com.example.recruitment.analysis;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeAnalysisReportRepository extends JpaRepository<ResumeAnalysisReport, String> {

    Optional<ResumeAnalysisReport> findTopByApplicationIdOrderByCreatedAtDesc(String applicationId);
}
