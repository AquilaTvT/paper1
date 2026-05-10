package com.mmvs.dto;

import java.time.Instant;

public record StreamEvent(
        String taskId,
        String eventType,
        String status,
        String stage,
        TokenMetricsDto tokenMetrics,
        String summaryDelta,
        Object completed,
        String error,
        Instant createdAt
) {

    public static StreamEvent status(String taskId, String status, String stage) {
        return new StreamEvent(taskId, "status", status, stage, null, null, null, null, Instant.now());
    }

    public static StreamEvent stage(String taskId, String status, String stage) {
        return new StreamEvent(taskId, "stage", status, stage, null, null, null, null, Instant.now());
    }

    public static StreamEvent tokenMetrics(String taskId, String stage, TokenMetricsDto tokenMetrics) {
        return new StreamEvent(taskId, "token_metrics", null, stage, tokenMetrics, null, null, null, Instant.now());
    }

    public static StreamEvent summaryDelta(String taskId, String delta) {
        return new StreamEvent(taskId, "summary_delta", "streaming", "summary_generation", null, delta, null, null, Instant.now());
    }

    public static StreamEvent completed(String taskId, Object completed) {
        return new StreamEvent(taskId, "completed", "finished", "finished", null, null, completed, null, Instant.now());
    }

    public static StreamEvent error(String taskId, String stage, String error) {
        return new StreamEvent(taskId, "error", "failed", stage, null, null, null, error, Instant.now());
    }
}
