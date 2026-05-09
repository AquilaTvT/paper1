package com.mmvs.dto;

import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskLog;
import com.mmvs.model.TaskStatus;
import java.time.Instant;
import java.util.List;

public record TaskResponse(
        String taskId,
        String videoId,
        String queryText,
        TaskStatus status,
        String currentStage,
        int progress,
        String summaryText,
        TokenMetricsDto tokenMetrics,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt,
        List<TaskLog> logs
) {

    public static TaskResponse from(InferenceTask task) {
        return new TaskResponse(
                task.getTaskId(),
                task.getVideoId(),
                task.getQueryText(),
                task.getStatus(),
                task.getCurrentStage(),
                task.getProgress(),
                task.getSummaryText(),
                task.getTokenMetrics(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getStartedAt(),
                task.getFinishedAt(),
                List.copyOf(task.getLogs())
        );
    }
}
