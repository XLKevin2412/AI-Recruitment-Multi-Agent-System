package com.example.recruitment.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthEndpoint healthEndpoint;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(
            HealthEndpoint healthEndpoint,
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory) {
        this.healthEndpoint = healthEndpoint;
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "ai-recruitment-backend");
        body.put("status", healthEndpoint.health().getStatus().getCode());
        body.put("timestamp", Instant.now().toString());
        body.put("components", Map.of(
                "mysql", mysqlStatus(),
                "redis", redisStatus()));
        return body;
    }

    private String mysqlStatus() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String redisStatus() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }
}
