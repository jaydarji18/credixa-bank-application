package com.credixa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Public endpoint for service health monitoring")
public class HealthController {

    @GetMapping
    @Operation(summary = "Get service health status", description = "Returns the current health status and version of the API")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Credixa Pro API",
                "version", "1.0",
                "timestamp", LocalDateTime.now()
        ));
    }
}
