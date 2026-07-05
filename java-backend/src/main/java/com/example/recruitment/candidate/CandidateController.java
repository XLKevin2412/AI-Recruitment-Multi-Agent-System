package com.example.recruitment.candidate;

import java.net.URI;

import com.example.recruitment.common.PageResponse;
import com.example.recruitment.common.ResourceNotFoundException;

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

@RestController
@RequestMapping("/candidates")
public class CandidateController {

    private final CandidateRepository candidateRepository;

    public CandidateController(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @GetMapping
    public PageResponse<Candidate> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(candidateRepository.findAll(PageRequest.of(Math.max(page, 1) - 1, Math.min(pageSize, 100))));
    }

    @GetMapping("/{id}")
    public Candidate get(@PathVariable String id) {
        return findCandidate(id);
    }

    @PostMapping
    public ResponseEntity<Candidate> create(@Valid @RequestBody Candidate candidate) {
        candidate.setId(null);
        Candidate saved = candidateRepository.save(candidate);
        return ResponseEntity.created(URI.create("/api/candidates/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Candidate update(@PathVariable String id, @Valid @RequestBody Candidate request) {
        Candidate candidate = findCandidate(id);
        candidate.setName(request.getName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setSource(request.getSource());
        candidate.setCurrentLocation(request.getCurrentLocation());
        return candidateRepository.save(candidate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Candidate candidate = findCandidate(id);
        candidateRepository.delete(candidate);
        return ResponseEntity.noContent().build();
    }

    private Candidate findCandidate(String id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", id));
    }
}
