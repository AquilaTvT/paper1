package com.mmvs.dto;

import com.mmvs.model.InferenceResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ResultResponse(
        String resultId,
        String taskId,
        String summary,
        List<String> keyEvents,
        TokenMetricsDto tokenMetrics,
        long estimatedLatencyMs,
        String scenarioType,
        Map<String, Object> lightSwitchAnalysis,
        String fallbackReason,
        Instant createdAt
) {

    public static ResultResponse from(InferenceResult result) {
        return new ResultResponse(
                result.getResultId(),
                result.getTaskId(),
                result.getSummary(),
                result.getKeyEvents(),
                result.getTokenMetrics(),
                result.getEstimatedLatencyMs(),
                result.getScenarioType(),
                result.getLightSwitchAnalysis(),
                result.getFallbackReason(),
                result.getCreatedAt()
        );
    }
}
