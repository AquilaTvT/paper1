package com.mmvs.controller;

import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.ApiResponse;
import com.mmvs.util.IdGenerator;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final InferenceProperties inferenceProperties;

    public HealthController(InferenceProperties inferenceProperties) {
        this.inferenceProperties = inferenceProperties;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "backend-java",
                "status", "up",
                "mode", inferenceProperties.getMode(),
                "redisEnabled", inferenceProperties.isRedisEnabled(),
                "h2JpaEnabled", inferenceProperties.isH2JpaEnabled(),
                "time", Instant.now().toString()
        ), IdGenerator.requestId());
    }
}
