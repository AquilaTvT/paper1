package com.mmvs.model;

import java.time.Instant;

public class TaskLog {

    private String logId;
    private String taskId;
    private String level;
    private String stage;
    private String message;
    private Instant createdAt;

    public TaskLog() {
    }

    public TaskLog(String logId, String taskId, String level, String stage, String message, Instant createdAt) {
        this.logId = logId;
        this.taskId = taskId;
        this.level = level;
        this.stage = stage;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
