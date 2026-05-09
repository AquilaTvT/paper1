package com.mmvs.model;

import com.mmvs.dto.TokenMetricsDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class InferenceTask {

    private String taskId;
    private String videoId;
    private String queryText;
    private TaskStatus status;
    private String currentStage;
    private int progress;
    private String summaryText = "";
    private TokenMetricsDto tokenMetrics;
    private InferenceResult result;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private final List<TaskLog> logs = new ArrayList<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void appendSummaryText(String delta) {
        this.summaryText = this.summaryText + delta;
    }

    public TokenMetricsDto getTokenMetrics() {
        return tokenMetrics;
    }

    public void setTokenMetrics(TokenMetricsDto tokenMetrics) {
        this.tokenMetrics = tokenMetrics;
    }

    public InferenceResult getResult() {
        return result;
    }

    public void setResult(InferenceResult result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<TaskLog> getLogs() {
        return logs;
    }

    public void addLog(TaskLog log) {
        this.logs.add(log);
    }
}
