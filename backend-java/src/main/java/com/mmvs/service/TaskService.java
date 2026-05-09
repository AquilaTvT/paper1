package com.mmvs.service;

import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.model.InferenceResult;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskStatus;
import java.util.List;

public interface TaskService {

    InferenceTask createTask(String videoId, String queryText);

    InferenceTask getTask(String taskId);

    List<InferenceTask> listTasks();

    InferenceTask updateStage(String taskId, TaskStatus status, String stage, int progress, String logMessage);

    InferenceTask setTokenMetrics(String taskId, TokenMetricsDto metrics);

    InferenceTask appendSummaryDelta(String taskId, String delta);

    InferenceResult completeTask(String taskId, String summary, List<String> keyEvents, long estimatedLatencyMs);

    InferenceTask failTask(String taskId, String stage, String errorMessage);

    InferenceResult getResult(String taskId);
}
