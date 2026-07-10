package com.example.recruitment.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.example.recruitment.agent.AgentRun;
import com.example.recruitment.agent.AgentRunRepository;
import com.example.recruitment.analysis.ResumeAnalysisReport;
import com.example.recruitment.analysis.ResumeAnalysisReportRepository;
import com.example.recruitment.common.BadRequestException;
import com.example.recruitment.common.ConflictException;
import com.example.recruitment.common.JsonLists;
import com.example.recruitment.common.ResourceNotFoundException;
import com.example.recruitment.email.EmailDraft;
import com.example.recruitment.email.EmailDraftRepository;
import com.example.recruitment.integration.AgentRuntimeClient;
import com.example.recruitment.integration.RagServiceClient;
import com.example.recruitment.interview.InterviewPlan;
import com.example.recruitment.interview.InterviewPlanRepository;
import com.example.recruitment.job.Job;
import com.example.recruitment.rag.RagDocument;
import com.example.recruitment.rag.RagDocumentRepository;
import com.example.recruitment.resume.Resume;
import com.example.recruitment.resume.ResumeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RecruitmentWorkflowService {

    static final long MAX_RESUME_SIZE_BYTES = 10L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = { '%', 'P', 'D', 'F', '-' };

    private final JobApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisReportRepository analysisReportRepository;
    private final AgentRunRepository agentRunRepository;
    private final EmailDraftRepository emailDraftRepository;
    private final InterviewPlanRepository interviewPlanRepository;
    private final RagDocumentRepository ragDocumentRepository;
    private final AgentRuntimeClient agentRuntimeClient;
    private final RagServiceClient ragServiceClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Path resumeStorageDir;

    public RecruitmentWorkflowService(
            JobApplicationRepository applicationRepository,
            ResumeRepository resumeRepository,
            ResumeAnalysisReportRepository analysisReportRepository,
            AgentRunRepository agentRunRepository,
            EmailDraftRepository emailDraftRepository,
            InterviewPlanRepository interviewPlanRepository,
            RagDocumentRepository ragDocumentRepository,
            AgentRuntimeClient agentRuntimeClient,
            RagServiceClient ragServiceClient,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            @Value("${storage.resume-dir:var/resumes}") String resumeStorageDir) {
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.agentRunRepository = agentRunRepository;
        this.emailDraftRepository = emailDraftRepository;
        this.interviewPlanRepository = interviewPlanRepository;
        this.ragDocumentRepository = ragDocumentRepository;
        this.agentRuntimeClient = agentRuntimeClient;
        this.ragServiceClient = ragServiceClient;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.resumeStorageDir = Path.of(resumeStorageDir);
    }

    @Transactional
    public ResumeUploadResponse uploadResume(String applicationId, MultipartFile file) throws IOException {
        JobApplication application = findApplication(applicationId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Resume file is required");
        }
        if (file.getSize() > MAX_RESUME_SIZE_BYTES) {
            throw new BadRequestException("Resume file must not exceed 10 MiB");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BadRequestException("Only PDF resumes are supported");
        }

        byte[] content = file.getBytes();
        if (!hasPdfSignature(content)) {
            throw new BadRequestException("Uploaded file is not a valid PDF");
        }

        Files.createDirectories(resumeStorageDir);
        String resumeId = UUID.randomUUID().toString();
        Path storagePath = resumeStorageDir.resolve(resumeId + ".pdf");
        Files.write(storagePath, content);

        String traceId = UUID.randomUUID().toString();
        AgentRuntimeClient.ResumeParseResponse parseResponse = agentRuntimeClient.parseResume(
                new AgentRuntimeClient.ResumeParseRequest(traceId, fileName, Base64.getEncoder().encodeToString(content)));

        Resume resume = new Resume();
        resume.setCandidateId(application.getCandidate().getId());
        resume.setFileName(fileName);
        resume.setFileType("PDF");
        resume.setStoragePath(storagePath.toString());
        resume.setParsedText(parseResponse.parsedText());
        resume.setLanguage(parseResponse.language());
        resume.setParseStatus(parseResponse.parseStatus());
        resume.setErrorMessage(parseResponse.errorMessage());
        resume = resumeRepository.save(resume);

        application.setResumeId(resume.getId());
        application.setStatus("PARSED".equals(parseResponse.parseStatus())
                ? ApplicationStatus.WAITING_ANALYSIS
                : ApplicationStatus.FAILED);
        application.setCurrentStage(application.getStatus().name());
        applicationRepository.save(application);
        return new ResumeUploadResponse(resume.getId(), resume.getParseStatus(), application.getStatus());
    }

    private static String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "resume.pdf";
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("\\p{Cntrl}", "_")
                .trim();
        if (fileName.isBlank()) {
            return "resume.pdf";
        }
        if (fileName.length() > 255) {
            String extension = fileName.toLowerCase(Locale.ROOT).endsWith(".pdf") ? ".pdf" : "";
            return fileName.substring(0, 255 - extension.length()) + extension;
        }
        return fileName;
    }

    private static boolean hasPdfSignature(byte[] content) {
        if (content.length < PDF_SIGNATURE.length) {
            return false;
        }
        int lastStart = Math.min(content.length - PDF_SIGNATURE.length, 1024 - PDF_SIGNATURE.length);
        for (int start = 0; start <= lastStart; start++) {
            boolean matches = true;
            for (int offset = 0; offset < PDF_SIGNATURE.length; offset++) {
                if (content[start + offset] != PDF_SIGNATURE[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public AnalysisTriggerResponse triggerAnalysis(String applicationId, boolean forceReanalyze) {
        JobApplication application = findApplication(applicationId);
        if (application.getStatus() == ApplicationStatus.ANALYSIS_COMPLETED && !forceReanalyze) {
            throw new ConflictException("Analysis already completed; set forceReanalyze=true to run again");
        }
        if (application.getResumeId() == null || application.getResumeId().isBlank()) {
            throw new BadRequestException("Application has no uploaded resume");
        }
        Resume resume = resumeRepository.findById(application.getResumeId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume", application.getResumeId()));
        if (!"PARSED".equals(resume.getParseStatus())) {
            throw new ConflictException("Resume is not parsed");
        }

        String traceId = UUID.randomUUID().toString();
        application.setStatus(ApplicationStatus.ANALYZING);
        application.setCurrentStage(ApplicationStatus.ANALYZING.name());
        applicationRepository.save(application);

        indexRagDocument(traceId, "RESUME", resume.getId(), application, resume.getParsedText());
        indexRagDocument(traceId, "JOB_REQUIREMENT", application.getJob().getId(), application, jobText(application.getJob()));
        RagServiceClient.RagSearchResponse searchResponse = ragServiceClient.search(
                new RagServiceClient.RagSearchRequest(
                        traceId,
                        jobText(application.getJob()),
                        List.of("RESUME", "JOB_REQUIREMENT"),
                        5,
                        Map.of("applicationId", application.getId(), "jobPositionId", application.getJob().getId())));

        List<AgentRuntimeClient.RagEvidence> evidence = searchResponse.items().stream()
                .map(item -> new AgentRuntimeClient.RagEvidence(item.evidenceId(), item.sourceType(), item.content(), item.score()))
                .toList();
        AgentRuntimeClient.ResumeAnalysisResponse response = agentRuntimeClient.analyzeResume(
                new AgentRuntimeClient.ResumeAnalysisRequest(
                        traceId,
                        application.getId(),
                        new AgentRuntimeClient.CandidatePayload(
                                application.getCandidate().getId(),
                                application.getCandidate().getName(),
                                application.getCandidate().getEmail()),
                        toJobPayload(application.getJob()),
                        new AgentRuntimeClient.ResumePayload(resume.getId(), resume.getParsedText()),
                        evidence));

        AgentRun agentRun = saveAgentRun(
                response.agentRunId(),
                application.getId(),
                "RESUME_ANALYZER",
                response.modelName(),
                "Resume analysis for application " + application.getId(),
                response.summary(),
                response.tokenUsage());

        ResumeAnalysisReport report = new ResumeAnalysisReport();
        report.setApplicationId(application.getId());
        report.setAgentRunId(agentRun.getId());
        report.setOverallScore(response.overallScore());
        report.setRecommendation(response.recommendation());
        report.setMatchedSkillsJson(JsonLists.writeStringList(objectMapper, response.matchedSkills()));
        report.setMissingSkillsJson(JsonLists.writeStringList(objectMapper, response.missingSkills()));
        report.setStrengthsJson(JsonLists.writeStringList(objectMapper, response.strengths()));
        report.setRisksJson(JsonLists.writeStringList(objectMapper, response.risks()));
        report.setSummary(response.summary());
        report.setRagEvidenceIdsJson(JsonLists.writeStringList(objectMapper, evidence.stream()
                .map(AgentRuntimeClient.RagEvidence::evidenceId)
                .toList()));
        report.setRequiresHumanReview(response.requiresHumanReview());
        report = analysisReportRepository.save(report);

        application.setStatus(ApplicationStatus.ANALYSIS_COMPLETED);
        application.setCurrentStage(ApplicationStatus.ANALYSIS_COMPLETED.name());
        applicationRepository.save(application);
        cache("ai-recruitment:analysis:latest:" + application.getId(), report.getId(), Duration.ofHours(1));
        return new AnalysisTriggerResponse(application.getId(), application.getStatus(), agentRun.getId(), report.getId());
    }

    @Transactional(readOnly = true)
    public ResumeAnalysisReport getLatestAnalysis(String applicationId) {
        findApplication(applicationId);
        return analysisReportRepository.findTopByApplicationIdOrderByCreatedAtDesc(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis report", applicationId));
    }

    @Transactional
    public EmailDraft draftEmail(String applicationId, String emailType) {
        JobApplication application = findApplication(applicationId);
        ResumeAnalysisReport report = getLatestAnalysis(applicationId);
        AgentRuntimeClient.EmailDraftResponse response = agentRuntimeClient.draftEmail(
                new AgentRuntimeClient.EmailDraftRequest(
                        UUID.randomUUID().toString(),
                        application.getId(),
                        emailType,
                        new AgentRuntimeClient.CandidatePayload(
                                application.getCandidate().getId(),
                                application.getCandidate().getName(),
                                application.getCandidate().getEmail()),
                        toJobPayload(application.getJob()),
                        analysisMap(report)));
        AgentRun agentRun = saveAgentRun(
                response.agentRunId(),
                application.getId(),
                "EMAIL_COMMUNICATION",
                response.modelName(),
                "Email draft " + emailType,
                response.subject(),
                response.tokenUsage());
        EmailDraft draft = new EmailDraft();
        draft.setApplicationId(application.getId());
        draft.setAgentRunId(agentRun.getId());
        draft.setEmailType(emailType);
        draft.setSubject(response.subject());
        draft.setBody(response.body());
        draft.setStatus("WAITING_REVIEW");
        draft = emailDraftRepository.save(draft);
        application.setStatus(ApplicationStatus.EMAIL_DRAFTED);
        application.setCurrentStage(ApplicationStatus.EMAIL_DRAFTED.name());
        applicationRepository.save(application);
        return draft;
    }

    @Transactional(readOnly = true)
    public List<EmailDraft> listEmails(String applicationId) {
        findApplication(applicationId);
        return emailDraftRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    @Transactional
    public InterviewPlan proposeInterview(String applicationId, InterviewPlanRequest request) {
        JobApplication application = findApplication(applicationId);
        ResumeAnalysisReport report = getLatestAnalysis(applicationId);
        List<AgentRuntimeClient.TimeSlot> slots = request.getCandidateAvailableSlots().stream()
                .map(slot -> new AgentRuntimeClient.TimeSlot(slot.getStartTime(), slot.getEndTime()))
                .toList();
        AgentRuntimeClient.InterviewPlanResponse response = agentRuntimeClient.planInterview(
                new AgentRuntimeClient.InterviewPlanRequest(
                        UUID.randomUUID().toString(),
                        application.getId(),
                        request.getInterviewType(),
                        request.getPreferredTimezone(),
                        slots,
                        toJobPayload(application.getJob()),
                        analysisMap(report)));
        AgentRun agentRun = saveAgentRun(
                response.agentRunId(),
                application.getId(),
                "INTERVIEW_SCHEDULER",
                response.modelName(),
                "Interview plan",
                response.meetingTitle(),
                response.tokenUsage());
        InterviewPlan plan = new InterviewPlan();
        plan.setApplicationId(application.getId());
        plan.setAgentRunId(agentRun.getId());
        plan.setInterviewType(request.getInterviewType());
        plan.setProposedStartTime(Instant.parse(response.proposedStartTime()));
        plan.setProposedEndTime(Instant.parse(response.proposedEndTime()));
        plan.setTimezone(response.timezone());
        plan.setMeetingTitle(response.meetingTitle());
        plan.setMeetingNotes(response.meetingNotes());
        plan.setStatus("WAITING_REVIEW");
        plan = interviewPlanRepository.save(plan);
        application.setStatus(ApplicationStatus.INTERVIEW_PROPOSED);
        application.setCurrentStage(ApplicationStatus.INTERVIEW_PROPOSED.name());
        applicationRepository.save(application);
        return plan;
    }

    @Transactional(readOnly = true)
    public List<InterviewPlan> listInterviews(String applicationId) {
        findApplication(applicationId);
        return interviewPlanRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    @Transactional(readOnly = true)
    public AgentRuntimeClient.QuestionAnswerResponse answerQuestion(String applicationId, String question) {
        JobApplication application = findApplication(applicationId);
        RagServiceClient.RagSearchResponse searchResponse = ragServiceClient.search(
                new RagServiceClient.RagSearchRequest(
                        UUID.randomUUID().toString(),
                        question,
                        List.of("RESUME", "JOB_REQUIREMENT"),
                        5,
                        Map.of("applicationId", application.getId(), "jobPositionId", application.getJob().getId())));
        List<AgentRuntimeClient.RagEvidence> evidence = searchResponse.items().stream()
                .map(item -> new AgentRuntimeClient.RagEvidence(item.evidenceId(), item.sourceType(), item.content(), item.score()))
                .toList();
        return agentRuntimeClient.answerQuestion(
                new AgentRuntimeClient.QuestionAnswerRequest(UUID.randomUUID().toString(), question, evidence));
    }

    private JobApplication findApplication(String applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    private void indexRagDocument(String traceId, String sourceType, String sourceId, JobApplication application, String content) {
        RagServiceClient.RagDocumentResponse response = ragServiceClient.indexDocument(
                new RagServiceClient.RagDocumentRequest(
                        traceId,
                        sourceType,
                        sourceId,
                        application.getId(),
                        application.getJob().getId(),
                        content,
                        Map.of()));
        RagDocument document = new RagDocument();
        document.setId(response.documentId());
        document.setSourceType(sourceType);
        document.setSourceId(sourceId);
        document.setApplicationId(application.getId());
        document.setJobPositionId(application.getJob().getId());
        document.setChunkCount(response.chunkCount());
        document.setStatus(response.status());
        document.setMetadataJson("{}");
        ragDocumentRepository.save(document);
    }

    private AgentRuntimeClient.JobPayload toJobPayload(Job job) {
        return new AgentRuntimeClient.JobPayload(
                job.getId(),
                job.getTitle(),
                JsonLists.readStringList(objectMapper, job.getRequiredSkillsJson()),
                JsonLists.readStringList(objectMapper, job.getPreferredSkillsJson()),
                job.getExperienceRequirement(),
                job.getDescription(),
                job.getInterviewRequirements());
    }

    private String jobText(Job job) {
        return String.join(" ",
                job.getTitle(),
                nullToEmpty(job.getRequiredSkillsJson()),
                nullToEmpty(job.getPreferredSkillsJson()),
                nullToEmpty(job.getExperienceRequirement()),
                nullToEmpty(job.getDescription()),
                nullToEmpty(job.getInterviewRequirements()));
    }

    private Map<String, Object> analysisMap(ResumeAnalysisReport report) {
        return Map.of(
                "id", report.getId(),
                "overallScore", report.getOverallScore(),
                "recommendation", report.getRecommendation(),
                "summary", report.getSummary(),
                "risks", JsonLists.readStringList(objectMapper, report.getRisksJson()));
    }

    private AgentRun saveAgentRun(
            String agentRunId,
            String applicationId,
            String agentType,
            String modelName,
            String inputSummary,
            String outputSummary,
            AgentRuntimeClient.TokenUsage tokenUsage) {
        AgentRun agentRun = new AgentRun();
        agentRun.setId(agentRunId);
        agentRun.setApplicationId(applicationId);
        agentRun.setAgentType(agentType);
        agentRun.setModelName(modelName);
        agentRun.setInputSummary(inputSummary);
        agentRun.setOutputSummary(outputSummary);
        agentRun.setStatus("SUCCEEDED");
        agentRun.setInputTokens(tokenUsage.inputTokens());
        agentRun.setOutputTokens(tokenUsage.outputTokens());
        agentRun.setTotalTokens(tokenUsage.totalTokens());
        return agentRunRepository.save(agentRun);
    }

    private void cache(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ignored) {
            // Cache failures must not break the recruitment workflow.
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
