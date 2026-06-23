USE ai_recruitment;

CREATE TABLE IF NOT EXISTS candidates (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NULL,
  phone VARCHAR(50) NULL,
  source VARCHAR(50) NULL,
  current_location VARCHAR(100) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_candidates_email (email),
  KEY idx_candidates_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_templates (
  id VARCHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  job_type VARCHAR(80) NOT NULL,
  required_skills_json JSON NOT NULL,
  preferred_skills_json JSON NULL,
  experience_requirement TEXT NULL,
  description TEXT NULL,
  interview_requirements TEXT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_job_templates_job_type (job_type),
  KEY idx_job_templates_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_positions (
  id VARCHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  job_type VARCHAR(80) NOT NULL,
  department VARCHAR(100) NULL,
  level VARCHAR(50) NULL,
  required_skills_json JSON NOT NULL,
  preferred_skills_json JSON NULL,
  experience_requirement TEXT NULL,
  description TEXT NULL,
  interview_requirements TEXT NULL,
  status VARCHAR(30) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_job_positions_status (status),
  KEY idx_job_positions_job_type (job_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resumes (
  id VARCHAR(36) PRIMARY KEY,
  candidate_id VARCHAR(36) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_type VARCHAR(30) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  parsed_text LONGTEXT NULL,
  language VARCHAR(20) NULL,
  parse_status VARCHAR(30) NOT NULL,
  error_message TEXT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_resumes_candidate_id (candidate_id),
  KEY idx_resumes_parse_status (parse_status),
  CONSTRAINT fk_resumes_candidate_id FOREIGN KEY (candidate_id) REFERENCES candidates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS applications (
  id VARCHAR(36) PRIMARY KEY,
  candidate_id VARCHAR(36) NOT NULL,
  job_position_id VARCHAR(36) NOT NULL,
  resume_id VARCHAR(36) NULL,
  status VARCHAR(50) NOT NULL,
  current_stage VARCHAR(50) NULL,
  submitted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_applications_candidate_job (candidate_id, job_position_id),
  KEY idx_applications_candidate_id (candidate_id),
  KEY idx_applications_job_position_id (job_position_id),
  KEY idx_applications_resume_id (resume_id),
  KEY idx_applications_status (status),
  CONSTRAINT fk_applications_candidate_id FOREIGN KEY (candidate_id) REFERENCES candidates (id),
  CONSTRAINT fk_applications_job_position_id FOREIGN KEY (job_position_id) REFERENCES job_positions (id),
  CONSTRAINT fk_applications_resume_id FOREIGN KEY (resume_id) REFERENCES resumes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_runs (
  id VARCHAR(36) PRIMARY KEY,
  application_id VARCHAR(36) NULL,
  agent_type VARCHAR(50) NOT NULL,
  model_name VARCHAR(100) NULL,
  input_summary TEXT NULL,
  output_summary TEXT NULL,
  status VARCHAR(30) NOT NULL,
  latency_ms INT NULL,
  input_tokens INT NULL,
  output_tokens INT NULL,
  total_tokens INT NULL,
  error_message TEXT NULL,
  trace_id VARCHAR(100) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_agent_runs_application_id (application_id),
  KEY idx_agent_runs_agent_type (agent_type),
  KEY idx_agent_runs_status (status),
  KEY idx_agent_runs_trace_id (trace_id),
  CONSTRAINT fk_agent_runs_application_id FOREIGN KEY (application_id) REFERENCES applications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resume_analysis_reports (
  id VARCHAR(36) PRIMARY KEY,
  application_id VARCHAR(36) NOT NULL,
  agent_run_id VARCHAR(36) NULL,
  overall_score INT NOT NULL,
  recommendation VARCHAR(50) NOT NULL,
  matched_skills_json JSON NULL,
  missing_skills_json JSON NULL,
  strengths_json JSON NULL,
  risks_json JSON NULL,
  summary TEXT NOT NULL,
  rag_evidence_ids_json JSON NULL,
  requires_human_review TINYINT(1) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_resume_analysis_reports_application_id (application_id),
  KEY idx_resume_analysis_reports_recommendation (recommendation),
  KEY idx_resume_analysis_reports_agent_run_id (agent_run_id),
  CONSTRAINT fk_resume_analysis_reports_application_id FOREIGN KEY (application_id) REFERENCES applications (id),
  CONSTRAINT fk_resume_analysis_reports_agent_run_id FOREIGN KEY (agent_run_id) REFERENCES agent_runs (id),
  CONSTRAINT chk_resume_analysis_reports_score CHECK (overall_score >= 0 AND overall_score <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_drafts (
  id VARCHAR(36) PRIMARY KEY,
  application_id VARCHAR(36) NOT NULL,
  agent_run_id VARCHAR(36) NULL,
  email_type VARCHAR(50) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  body LONGTEXT NOT NULL,
  status VARCHAR(30) NOT NULL,
  reviewed_by VARCHAR(36) NULL,
  reviewed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_email_drafts_application_id (application_id),
  KEY idx_email_drafts_status (status),
  KEY idx_email_drafts_agent_run_id (agent_run_id),
  CONSTRAINT fk_email_drafts_application_id FOREIGN KEY (application_id) REFERENCES applications (id),
  CONSTRAINT fk_email_drafts_agent_run_id FOREIGN KEY (agent_run_id) REFERENCES agent_runs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_plans (
  id VARCHAR(36) PRIMARY KEY,
  application_id VARCHAR(36) NOT NULL,
  agent_run_id VARCHAR(36) NULL,
  interview_type VARCHAR(50) NOT NULL,
  proposed_start_time DATETIME(3) NOT NULL,
  proposed_end_time DATETIME(3) NOT NULL,
  timezone VARCHAR(50) NOT NULL,
  meeting_title VARCHAR(255) NOT NULL,
  meeting_notes TEXT NULL,
  status VARCHAR(30) NOT NULL,
  reviewed_by VARCHAR(36) NULL,
  reviewed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_interview_plans_application_id (application_id),
  KEY idx_interview_plans_status (status),
  KEY idx_interview_plans_start_time (proposed_start_time),
  KEY idx_interview_plans_agent_run_id (agent_run_id),
  CONSTRAINT fk_interview_plans_application_id FOREIGN KEY (application_id) REFERENCES applications (id),
  CONSTRAINT fk_interview_plans_agent_run_id FOREIGN KEY (agent_run_id) REFERENCES agent_runs (id),
  CONSTRAINT chk_interview_plans_time CHECK (proposed_end_time > proposed_start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rag_documents (
  id VARCHAR(36) PRIMARY KEY,
  source_type VARCHAR(50) NOT NULL,
  source_id VARCHAR(36) NOT NULL,
  application_id VARCHAR(36) NULL,
  job_position_id VARCHAR(36) NULL,
  chunk_count INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  metadata_json JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_rag_documents_source (source_type, source_id),
  KEY idx_rag_documents_application_id (application_id),
  KEY idx_rag_documents_job_position_id (job_position_id),
  CONSTRAINT fk_rag_documents_application_id FOREIGN KEY (application_id) REFERENCES applications (id),
  CONSTRAINT fk_rag_documents_job_position_id FOREIGN KEY (job_position_id) REFERENCES job_positions (id),
  CONSTRAINT chk_rag_documents_chunk_count CHECK (chunk_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS human_review_records (
  id VARCHAR(36) PRIMARY KEY,
  application_id VARCHAR(36) NOT NULL,
  review_type VARCHAR(50) NOT NULL,
  target_id VARCHAR(36) NOT NULL,
  decision VARCHAR(50) NOT NULL,
  comment TEXT NULL,
  reviewed_by VARCHAR(36) NULL,
  reviewed_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_human_review_records_application_id (application_id),
  KEY idx_human_review_records_target (review_type, target_id),
  CONSTRAINT fk_human_review_records_application_id FOREIGN KEY (application_id) REFERENCES applications (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

