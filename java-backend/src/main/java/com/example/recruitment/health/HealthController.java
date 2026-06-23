package com.example.recruitment.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    public HealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
                "service", "ai-recruitment-backend",
                "status", healthEndpoint.health().getStatus().getCode(),
                "timestamp", Instant.now().toString());
    }
}
