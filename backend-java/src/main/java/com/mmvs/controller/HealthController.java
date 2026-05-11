package com.mmvs.controller;

import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.ApiResponse;
import com.mmvs.service.PythonInferenceScheduler;
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
    private final PythonInferenceScheduler pythonInferenceScheduler;

    public HealthController(InferenceProperties inferenceProperties, PythonInferenceScheduler pythonInferenceScheduler) {
        this.inferenceProperties = inferenceProperties;
        this.pythonInferenceScheduler = pythonInferenceScheduler;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service", "backend-java",
                "status", "up",
                "mode", inferenceProperties.getMode(),
                "pythonConnected", pythonInferenceScheduler.isPythonHealthy(),
                "time", Instant.now().toString()
        ), IdGenerator.requestId());
    }
}
