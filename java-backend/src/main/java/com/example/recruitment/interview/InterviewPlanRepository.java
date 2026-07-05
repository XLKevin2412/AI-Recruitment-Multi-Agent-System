package com.example.recruitment.interview;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewPlanRepository extends JpaRepository<InterviewPlan, String> {

    List<InterviewPlan> findByApplicationIdOrderByCreatedAtDesc(String applicationId);
}
