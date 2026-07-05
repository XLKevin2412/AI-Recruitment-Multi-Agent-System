package com.example.recruitment.email;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailDraftRepository extends JpaRepository<EmailDraft, String> {

    List<EmailDraft> findByApplicationIdOrderByCreatedAtDesc(String applicationId);
}
