package com.mmvs.controller;

import com.mmvs.dto.ApiResponse;
import com.mmvs.dto.CreateTaskRequest;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TaskResponse;
import com.mmvs.model.InferenceTask;
import com.mmvs.service.TaskDispatchService;
import com.mmvs.service.SseEmitterService;
import com.mmvs.service.TaskService;
import com.mmvs.util.IdGenerator;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskDispatchService taskDispatchService;
    private final SseEmitterService sseEmitterService;

    public TaskController(
            TaskService taskService,
            TaskDispatchService taskDispatchService,
            SseEmitterService sseEmitterService
    ) {
        this.taskService = taskService;
        this.taskDispatchService = taskDispatchService;
        this.sseEmitterService = sseEmitterService;
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        InferenceTask task = taskService.createTask(request.videoId(), request.effectiveQueryText());
        taskDispatchService.dispatch(task.getTaskId(), request.formalAnalysisRequested());
        return ApiResponse.ok(TaskResponse.from(task), IdGenerator.requestId());
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> listTasks() {
        List<TaskResponse> tasks = taskService.listTasks().stream()
                .map(TaskResponse::from)
                .toList();
        return ApiResponse.ok(tasks, IdGenerator.requestId());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@PathVariable String taskId) {
        return ApiResponse.ok(TaskResponse.from(taskService.getTask(taskId)), IdGenerator.requestId());
    }

    @GetMapping(value = "/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String taskId) {
        InferenceTask task = taskService.getTask(taskId);
        SseEmitter emitter = sseEmitterService.subscribe(taskId);
        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(StreamEvent.status(taskId, task.getStatus().getValue(), task.getCurrentStage())));
            if (task.getErrorMessage() != null && !task.getErrorMessage().isBlank()) {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(StreamEvent.error(taskId, task.getCurrentStage(), task.getErrorMessage())));
            }
        } catch (Exception ignored) {
            emitter.complete();
        }
        return emitter;
    }
}
