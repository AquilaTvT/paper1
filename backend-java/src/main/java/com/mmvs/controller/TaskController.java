package com.mmvs.controller;

import com.mmvs.dto.ApiResponse;
import com.mmvs.dto.CreateTaskRequest;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TaskResponse;
import com.mmvs.model.InferenceTask;
import com.mmvs.config.AppModeProperties;
import com.mmvs.service.MockInferenceScheduler;
import com.mmvs.service.RedisStreamSseService;
import com.mmvs.service.SseEmitterService;
import com.mmvs.service.TaskService;
import com.mmvs.util.IdGenerator;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<MockInferenceScheduler> mockInferenceScheduler;
    private final SseEmitterService sseEmitterService;
    private final ObjectProvider<RedisStreamSseService> redisStreamSseService;
    private final AppModeProperties appModeProperties;

    public TaskController(
            TaskService taskService,
            ObjectProvider<MockInferenceScheduler> mockInferenceScheduler,
            SseEmitterService sseEmitterService,
            ObjectProvider<RedisStreamSseService> redisStreamSseService,
            AppModeProperties appModeProperties
    ) {
        this.taskService = taskService;
        this.mockInferenceScheduler = mockInferenceScheduler;
        this.sseEmitterService = sseEmitterService;
        this.redisStreamSseService = redisStreamSseService;
        this.appModeProperties = appModeProperties;
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        InferenceTask task = taskService.createTask(request.videoId(), request.effectiveQueryText());
        mockInferenceScheduler.ifAvailable(scheduler -> scheduler.schedule(task.getTaskId()));
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
        if (appModeProperties.isRedisMode()) {
            return redisStreamSseService.getObject().subscribe(taskId);
        }
        SseEmitter emitter = sseEmitterService.subscribe(taskId);
        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(StreamEvent.of(taskId, "status", task.getCurrentStage(), Map.of(
                            "status", task.getStatus().getValue(),
                            "progress", task.getProgress()
                    ))));
        } catch (Exception ignored) {
            emitter.complete();
        }
        return emitter;
    }
}
