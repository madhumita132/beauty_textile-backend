package com.beautytextile.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * GET /actuator/health — used by Render as health-check endpoint.
 * Also exposes a simple GET /health for load-balancer pings.
 */
@RestController
@RequestMapping
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "Beauty Textile API",
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
