package com.mmvs.model;

import com.mmvs.dto.TokenMetricsDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InferenceResult {

    private String resultId;
    private String taskId;
    private String summary;
    private List<String> keyEvents = new ArrayList<>();
    private TokenMetricsDto tokenMetrics;
    private long estimatedLatencyMs;
    private Instant createdAt;

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeyEvents() {
        return keyEvents;
    }

    public void setKeyEvents(List<String> keyEvents) {
        this.keyEvents = keyEvents;
    }

    public TokenMetricsDto getTokenMetrics() {
        return tokenMetrics;
    }

    public void setTokenMetrics(TokenMetricsDto tokenMetrics) {
        this.tokenMetrics = tokenMetrics;
    }

    public long getEstimatedLatencyMs() {
        return estimatedLatencyMs;
    }

    public void setEstimatedLatencyMs(long estimatedLatencyMs) {
        this.estimatedLatencyMs = estimatedLatencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
