package com.example.recruitment.application;

import java.net.URI;
import java.util.List;

import com.example.recruitment.candidate.Candidate;
import com.example.recruitment.candidate.CandidateRepository;
import com.example.recruitment.common.ResourceNotFoundException;
import com.example.recruitment.job.Job;
import com.example.recruitment.job.JobRepository;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class JobApplicationController {

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;

    public JobApplicationController(
            JobApplicationRepository applicationRepository,
            JobRepository jobRepository,
            CandidateRepository candidateRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
    }

    @GetMapping
    public List<JobApplication> list() {
        return applicationRepository.findAll();
    }

    @GetMapping("/{id}")
    public JobApplication get(@PathVariable Long id) {
        return findApplication(id);
    }

    @PostMapping
    public ResponseEntity<JobApplication> create(@Valid @RequestBody JobApplicationRequest request) {
        JobApplication application = new JobApplication();
        applyRequest(application, request);
        JobApplication saved = applicationRepository.save(application);
        return ResponseEntity.created(URI.create("/api/applications/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public JobApplication update(@PathVariable Long id, @Valid @RequestBody JobApplicationRequest request) {
        JobApplication application = findApplication(id);
        applyRequest(application, request);
        return applicationRepository.save(application);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        JobApplication application = findApplication(id);
        applicationRepository.delete(application);
        return ResponseEntity.noContent().build();
    }

    private void applyRequest(JobApplication application, JobApplicationRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", request.getJobId()));
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", request.getCandidateId()));
        application.setJob(job);
        application.setCandidate(candidate);
        application.setStatus(request.getStatus());
        application.setResumeScore(request.getResumeScore());
        application.setScreeningSummary(request.getScreeningSummary());
    }

    private JobApplication findApplication(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
    }
}
