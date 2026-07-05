package com.example.recruitment.application;

import java.net.URI;

import com.example.recruitment.common.PageResponse;
import com.example.recruitment.candidate.Candidate;
import com.example.recruitment.candidate.CandidateRepository;
import com.example.recruitment.common.ResourceNotFoundException;
import com.example.recruitment.analysis.ResumeAnalysisReport;
import com.example.recruitment.email.EmailDraft;
import com.example.recruitment.integration.AgentRuntimeClient;
import com.example.recruitment.interview.InterviewPlan;
import com.example.recruitment.job.Job;
import com.example.recruitment.job.JobRepository;
import com.example.recruitment.resume.ResumeRepository;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/applications")
public class JobApplicationController {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationStatusTransitionValidator statusTransitionValidator;
    private final RecruitmentWorkflowService workflowService;

    public JobApplicationController(
            JobApplicationRepository applicationRepository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ResumeRepository resumeRepository,
            ApplicationStatusTransitionValidator statusTransitionValidator,
            RecruitmentWorkflowService workflowService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
        this.resumeRepository = resumeRepository;
        this.statusTransitionValidator = statusTransitionValidator;
        this.workflowService = workflowService;
    }

    @GetMapping
    public PageResponse<JobApplication> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(applicationRepository.findAll(PageRequest.of(Math.max(page, 1) - 1, Math.min(pageSize, 100))));
    }

    @GetMapping("/{id}")
    public JobApplication get(@PathVariable String id) {
        return findApplication(id);
    }

    @PostMapping
    public ResponseEntity<JobApplication> create(@Valid @RequestBody JobApplicationRequest request) {
        statusTransitionValidator.validateInitial(request.getStatus());
        JobApplication application = new JobApplication();
        applyCreateRequest(application, request);
        JobApplication saved = applicationRepository.save(application);
        return ResponseEntity.created(URI.create("/api/applications/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public JobApplication update(@PathVariable String id, @Valid @RequestBody JobApplicationUpdateRequest request) {
        JobApplication application = findApplication(id);
        statusTransitionValidator.validate(application.getStatus(), request.getStatus());
        applyUpdateRequest(application, request);
        return applicationRepository.save(application);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        JobApplication application = findApplication(id);
        applicationRepository.delete(application);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resume")
    public ResumeUploadResponse uploadResume(@PathVariable String id, @RequestParam("file") MultipartFile file)
            throws Exception {
        return workflowService.uploadResume(id, file);
    }

    @PostMapping("/{id}/analysis")
    public AnalysisTriggerResponse triggerAnalysis(
            @PathVariable String id,
            @RequestBody(required = false) AnalysisTriggerRequest request) {
        return workflowService.triggerAnalysis(id, request != null && request.isForceReanalyze());
    }

    @GetMapping("/{id}/analysis")
    public ResumeAnalysisReport getAnalysis(@PathVariable String id) {
        return workflowService.getLatestAnalysis(id);
    }

    @PostMapping("/{id}/emails/draft")
    public EmailDraft draftEmail(@PathVariable String id, @Valid @RequestBody EmailDraftRequest request) {
        return workflowService.draftEmail(id, request.getEmailType());
    }

    @GetMapping("/{id}/emails")
    public java.util.List<EmailDraft> listEmails(@PathVariable String id) {
        return workflowService.listEmails(id);
    }

    @PostMapping("/{id}/interviews/propose")
    public InterviewPlan proposeInterview(@PathVariable String id, @RequestBody InterviewPlanRequest request) {
        return workflowService.proposeInterview(id, request == null ? new InterviewPlanRequest() : request);
    }

    @GetMapping("/{id}/interviews")
    public java.util.List<InterviewPlan> listInterviews(@PathVariable String id) {
        return workflowService.listInterviews(id);
    }

    @PostMapping("/{id}/qa")
    public AgentRuntimeClient.QuestionAnswerResponse answerQuestion(
            @PathVariable String id,
            @Valid @RequestBody QuestionAnswerRequest request) {
        return workflowService.answerQuestion(id, request.getQuestion());
    }

    private void applyCreateRequest(JobApplication application, JobApplicationRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", request.getJobId()));
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", request.getCandidateId()));
        validateResumeExists(request.getResumeId());
        application.setJob(job);
        application.setCandidate(candidate);
        application.setStatus(request.getStatus());
        application.setResumeId(request.getResumeId());
        application.setCurrentStage(request.getCurrentStage());
    }

    private void applyUpdateRequest(JobApplication application, JobApplicationUpdateRequest request) {
        validateResumeExists(request.getResumeId());
        application.setStatus(request.getStatus());
        application.setResumeId(request.getResumeId());
        application.setCurrentStage(request.getCurrentStage());
    }

    private void validateResumeExists(String resumeId) {
        if (resumeId != null && !resumeId.isBlank()) {
            resumeRepository.findById(resumeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        }
    }

    private JobApplication findApplication(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }
}
