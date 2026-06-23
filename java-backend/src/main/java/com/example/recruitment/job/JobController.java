package com.example.recruitment.job;

import java.net.URI;
import java.util.List;

import com.example.recruitment.common.ResourceNotFoundException;

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
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public List<Job> list() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public Job get(@PathVariable Long id) {
        return findJob(id);
    }

    @PostMapping
    public ResponseEntity<Job> create(@Valid @RequestBody Job job) {
        job.setId(null);
        Job saved = jobRepository.save(job);
        return ResponseEntity.created(URI.create("/api/jobs/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Job update(@PathVariable Long id, @Valid @RequestBody Job request) {
        Job job = findJob(id);
        job.setTitle(request.getTitle());
        job.setDepartment(request.getDepartment());
        job.setLocation(request.getLocation());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setStatus(request.getStatus());
        return jobRepository.save(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Job job = findJob(id);
        jobRepository.delete(job);
        return ResponseEntity.noContent().build();
    }

    private Job findJob(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }
}
