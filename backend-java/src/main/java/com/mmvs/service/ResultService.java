package com.mmvs.service;

import com.mmvs.model.InferenceResult;
import org.springframework.stereotype.Service;

@Service
public class ResultService {

    private final TaskService taskService;

    public ResultService(TaskService taskService) {
        this.taskService = taskService;
    }

    public InferenceResult getResult(String taskId) {
        return taskService.getResult(taskId);
    }
}
