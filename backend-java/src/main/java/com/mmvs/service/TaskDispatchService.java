package com.mmvs.service;

import com.mmvs.config.InferenceProperties;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.VideoFile;
import java.util.Map;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskDispatchService {

    private final TaskService taskService;
    private final VideoStorageService videoStorageService;
    private final MockInferenceScheduler mockInferenceScheduler;
    private final StringRedisTemplate redisTemplate;
    private final InferenceProperties inferenceProperties;

    public TaskDispatchService(
            TaskService taskService,
            VideoStorageService videoStorageService,
            MockInferenceScheduler mockInferenceScheduler,
            StringRedisTemplate redisTemplate,
            InferenceProperties inferenceProperties
    ) {
        this.taskService = taskService;
        this.videoStorageService = videoStorageService;
        this.mockInferenceScheduler = mockInferenceScheduler;
        this.redisTemplate = redisTemplate;
        this.inferenceProperties = inferenceProperties;
    }

    public void dispatch(String taskId) {
        if (!inferenceProperties.isRedisMode()) {
            mockInferenceScheduler.schedule(taskId);
            return;
        }

        InferenceTask task = taskService.getTask(taskId);
        VideoFile videoFile = videoStorageService.getRequiredVideo(task.getVideoId());
        redisTemplate.opsForStream().add(MapRecord.create(
                inferenceProperties.getRedisTaskStream(),
                Map.of(
                        "taskId", task.getTaskId(),
                        "videoId", task.getVideoId(),
                        "videoPath", videoFile.getStoredPath(),
                        "queryText", task.getQueryText()
                )
        ));
        taskService.updateStage(taskId, task.getStatus(), "waiting", 5, "任务已写入 Redis Stream，等待 Python worker 消费");
    }
}
