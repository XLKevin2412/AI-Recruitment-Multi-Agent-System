package com.example.recruitment.candidate;

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
@RequestMapping("/candidates")
public class CandidateController {

    private final CandidateRepository candidateRepository;

    public CandidateController(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @GetMapping
    public List<Candidate> list() {
        return candidateRepository.findAll();
    }

    @GetMapping("/{id}")
    public Candidate get(@PathVariable Long id) {
        return findCandidate(id);
    }

    @PostMapping
    public ResponseEntity<Candidate> create(@Valid @RequestBody Candidate candidate) {
        candidate.setId(null);
        Candidate saved = candidateRepository.save(candidate);
        return ResponseEntity.created(URI.create("/api/candidates/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Candidate update(@PathVariable Long id, @Valid @RequestBody Candidate request) {
        Candidate candidate = findCandidate(id);
        candidate.setName(request.getName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setSkills(request.getSkills());
        candidate.setStatus(request.getStatus());
        return candidateRepository.save(candidate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Candidate candidate = findCandidate(id);
        candidateRepository.delete(candidate);
        return ResponseEntity.noContent().build();
    }

    private Candidate findCandidate(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", id));
    }
}
