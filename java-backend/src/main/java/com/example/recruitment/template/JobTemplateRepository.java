package com.example.recruitment.template;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JobTemplateRepository extends JpaRepository<JobTemplate, String> {

    List<JobTemplate> findByActiveTrueOrderByJobTypeAsc();
}
