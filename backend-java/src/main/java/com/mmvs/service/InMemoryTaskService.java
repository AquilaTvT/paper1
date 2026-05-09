package com.mmvs.service;

import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.exception.BusinessException;
import com.mmvs.model.InferenceResult;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskLog;
import com.mmvs.model.TaskStatus;
import com.mmvs.util.IdGenerator;
import com.mmvs.util.TimeUtils;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryTaskService implements TaskService {

    private final VideoStorageService videoStorageService;
    private final Map<String, InferenceTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, InferenceResult> results = new ConcurrentHashMap<>();

    public InMemoryTaskService(VideoStorageService videoStorageService) {
        this.videoStorageService = videoStorageService;
    }

    @Override
    public InferenceTask createTask(String videoId, String queryText) {
        videoStorageService.getRequiredVideo(videoId);
        Instant now = TimeUtils.now();
        InferenceTask task = new InferenceTask();
        task.setTaskId(IdGenerator.taskId());
        task.setVideoId(videoId);
        task.setQueryText(queryText);
        task.setStatus(TaskStatus.WAITING);
        task.setCurrentStage("waiting");
        task.setProgress(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        addLog(task, "INFO", "waiting", "任务已创建，进入 waiting 状态");
        tasks.put(task.getTaskId(), task);
        return task;
    }

    @Override
    public InferenceTask getTask(String taskId) {
        InferenceTask task = tasks.get(taskId);
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND", "任务不存在：" + taskId, HttpStatus.NOT_FOUND);
        }
        return task;
    }

    @Override
    public List<InferenceTask> listTasks() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(InferenceTask::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public InferenceTask updateStage(String taskId, TaskStatus status, String stage, int progress, String logMessage) {
        InferenceTask task = getTask(taskId);
        synchronized (task) {
            Instant now = TimeUtils.now();
            task.setStatus(status);
            task.setCurrentStage(stage);
            task.setProgress(progress);
            task.setUpdatedAt(now);
            if (status == TaskStatus.RUNNING && task.getStartedAt() == null) {
                task.setStartedAt(now);
            }
            addLog(task, "INFO", stage, logMessage);
        }
        return task;
    }

    @Override
    public InferenceTask setTokenMetrics(String taskId, TokenMetricsDto metrics) {
        InferenceTask task = getTask(taskId);
        synchronized (task) {
            task.setTokenMetrics(metrics);
            task.setUpdatedAt(TimeUtils.now());
            addLog(task, "INFO", "token_metrics", "Token 指标已生成：196 → 5");
        }
        return task;
    }

    @Override
    public InferenceTask appendSummaryDelta(String taskId, String delta) {
        InferenceTask task = getTask(taskId);
        synchronized (task) {
            task.appendSummaryText(delta);
            task.setStatus(TaskStatus.STREAMING);
            task.setCurrentStage("summary_generation");
            task.setUpdatedAt(TimeUtils.now());
            addLog(task, "INFO", "summary_generation", "输出摘要片段：" + delta);
        }
        return task;
    }

    @Override
    public InferenceResult completeTask(String taskId, String summary, List<String> keyEvents, long estimatedLatencyMs) {
        InferenceTask task = getTask(taskId);
        InferenceResult result = new InferenceResult();
        synchronized (task) {
            Instant now = TimeUtils.now();
            result.setResultId(IdGenerator.resultId());
            result.setTaskId(taskId);
            result.setSummary(summary);
            result.setKeyEvents(keyEvents);
            result.setTokenMetrics(task.getTokenMetrics());
            result.setEstimatedLatencyMs(estimatedLatencyMs);
            result.setCreatedAt(now);
            task.setResult(result);
            task.setStatus(TaskStatus.FINISHED);
            task.setCurrentStage("finished");
            task.setProgress(100);
            task.setUpdatedAt(now);
            task.setFinishedAt(now);
            addLog(task, "INFO", "finished", "任务完成，摘要结果已写入 in-memory 存储");
            results.put(taskId, result);
        }
        return result;
    }

    @Override
    public InferenceTask failTask(String taskId, String stage, String errorMessage) {
        InferenceTask task = getTask(taskId);
        synchronized (task) {
            Instant now = TimeUtils.now();
            task.setStatus(TaskStatus.FAILED);
            task.setCurrentStage(stage);
            task.setErrorMessage(errorMessage);
            task.setUpdatedAt(now);
            task.setFinishedAt(now);
            addLog(task, "ERROR", stage, errorMessage);
        }
        return task;
    }

    @Override
    public InferenceResult getResult(String taskId) {
        getTask(taskId);
        InferenceResult result = results.get(taskId);
        if (result == null) {
            throw new BusinessException("RESULT_NOT_READY", "推理结果尚未生成：" + taskId, HttpStatus.CONFLICT);
        }
        return result;
    }

    private void addLog(InferenceTask task, String level, String stage, String message) {
        task.addLog(new TaskLog(IdGenerator.logId(), task.getTaskId(), level, stage, message, TimeUtils.now()));
    }
}
