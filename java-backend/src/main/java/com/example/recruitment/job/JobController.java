package com.example.recruitment.job;

import java.net.URI;

import com.example.recruitment.common.PageResponse;
import com.example.recruitment.common.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
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

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JobController(JobRepository jobRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public PageResponse<Job> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(jobRepository.findAll(PageRequest.of(Math.max(page, 1) - 1, Math.min(pageSize, 100))));
    }

    @GetMapping("/{id}")
    public Job get(@PathVariable String id) {
        String cacheKey = "ai-recruitment:job:" + id;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, Job.class);
            }
        } catch (Exception ignored) {
            // Cache read failures fall through to MySQL.
        }
        Job job = findJob(id);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(job));
        } catch (Exception ignored) {
            // Cache write failures must not affect the API response.
        }
        return job;
    }

    @PostMapping
    public ResponseEntity<Job> create(@Valid @RequestBody Job job) {
        job.setId(null);
        Job saved = jobRepository.save(job);
        return ResponseEntity.created(URI.create("/api/jobs/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Job update(@PathVariable String id, @Valid @RequestBody Job request) {
        Job job = findJob(id);
        job.setTitle(request.getTitle());
        job.setJobType(request.getJobType());
        job.setDepartment(request.getDepartment());
        job.setLevel(request.getLevel());
        job.setRequiredSkillsJson(request.getRequiredSkillsJson());
        job.setPreferredSkillsJson(request.getPreferredSkillsJson());
        job.setExperienceRequirement(request.getExperienceRequirement());
        job.setDescription(request.getDescription());
        job.setInterviewRequirements(request.getInterviewRequirements());
        job.setStatus(request.getStatus());
        return jobRepository.save(job);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Job job = findJob(id);
        jobRepository.delete(job);
        return ResponseEntity.noContent().build();
    }

    private Job findJob(String id) {
        return jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job", id));
    }
}
