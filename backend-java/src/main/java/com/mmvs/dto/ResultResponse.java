package com.mmvs.dto;

import com.mmvs.model.InferenceResult;
import java.time.Instant;
import java.util.List;

public record ResultResponse(
        String resultId,
        String taskId,
        String summary,
        List<String> keyEvents,
        TokenMetricsDto tokenMetrics,
        long estimatedLatencyMs,
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
                result.getCreatedAt()
        );
    }
}
