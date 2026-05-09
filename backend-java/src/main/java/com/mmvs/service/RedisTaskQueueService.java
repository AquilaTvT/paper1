package com.mmvs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.exception.BusinessException;
import com.mmvs.model.InferenceResult;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskLog;
import com.mmvs.model.TaskStatus;
import com.mmvs.model.VideoFile;
import com.mmvs.util.IdGenerator;
import com.mmvs.util.TimeUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "redis")
public class RedisTaskQueueService implements TaskService {
    public static final String TASK_QUEUE = "queue:mmvs:tasks";

    private final StringRedisTemplate redisTemplate;
    private final VideoStorageService videoStorageService;
    private final ObjectMapper objectMapper;

    public RedisTaskQueueService(StringRedisTemplate redisTemplate, VideoStorageService videoStorageService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.videoStorageService = videoStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public InferenceTask createTask(String videoId, String queryText) {
        try {
            VideoFile video = videoStorageService.getRequiredVideo(videoId);
            String taskId = IdGenerator.taskId();
            String now = TimeUtils.now().toString();
            Map<String, String> state = new HashMap<>();
            state.put("taskId", taskId);
            state.put("videoId", videoId);
            state.put("videoPath", video.getStoredPath());
            state.put("queryText", queryText);
            state.put("status", "waiting");
            state.put("currentStage", "waiting");
            state.put("progress", "0");
            state.put("createdAt", now);
            state.put("updatedAt", now);
            redisTemplate.opsForHash().putAll(taskKey(taskId), state);
            redisTemplate.opsForStream().add(MapRecord.create(TASK_QUEUE, state));
            return getTask(taskId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("REDIS_UNAVAILABLE", "Redis 不可用，无法创建任务：" + exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public InferenceTask getTask(String taskId) {
        try {
            Map<Object, Object> raw = redisTemplate.opsForHash().entries(taskKey(taskId));
            if (raw.isEmpty()) {
                throw new BusinessException("TASK_NOT_FOUND", "任务不存在：" + taskId, HttpStatus.NOT_FOUND);
            }
            return toTask(raw);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("REDIS_UNAVAILABLE", "Redis 不可用，无法读取任务：" + exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public List<InferenceTask> listTasks() {
        try {
            java.util.Set<String> keys = redisTemplate.keys("task:*");
            if (keys == null) {
                return List.of();
            }
            return keys.stream()
                    .map(key -> key.substring("task:".length()))
                    .map(this::getTask)
                    .toList();
        } catch (Exception exception) {
            throw new BusinessException("REDIS_UNAVAILABLE", "Redis 不可用，无法查询任务列表：" + exception.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Override public InferenceTask updateStage(String taskId, TaskStatus status, String stage, int progress, String logMessage) { updateHash(taskId, Map.of("status", status.getValue(), "currentStage", stage, "progress", String.valueOf(progress), "updatedAt", TimeUtils.now().toString())); return getTask(taskId); }
    @Override public InferenceTask setTokenMetrics(String taskId, TokenMetricsDto metrics) { try { redisTemplate.opsForHash().put(taskKey(taskId), "tokenMetrics", objectMapper.writeValueAsString(metrics)); } catch (Exception ignored) { } return getTask(taskId); }
    @Override public InferenceTask appendSummaryDelta(String taskId, String delta) { redisTemplate.opsForHash().put(taskKey(taskId), "summaryText", getTask(taskId).getSummaryText() + delta); return getTask(taskId); }
    @Override public InferenceResult completeTask(String taskId, String summary, List<String> keyEvents, long estimatedLatencyMs) { updateHash(taskId, Map.of("status", "finished", "currentStage", "finished", "progress", "100", "updatedAt", TimeUtils.now().toString())); return getResult(taskId); }
    @Override public InferenceTask failTask(String taskId, String stage, String errorMessage) { updateHash(taskId, Map.of("status", "failed", "currentStage", stage, "errorMessage", errorMessage, "updatedAt", TimeUtils.now().toString())); return getTask(taskId); }

    @Override
    public InferenceResult getResult(String taskId) {
        String json = redisTemplate.opsForValue().get("result:" + taskId);
        if (json == null) {
            throw new BusinessException("RESULT_NOT_READY", "推理结果尚未生成：" + taskId, HttpStatus.CONFLICT);
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            InferenceResult result = new InferenceResult();
            result.setResultId("result-" + taskId);
            result.setTaskId(taskId);
            result.setSummary(root.path("summary").asText(""));
            if (root.has("key_events")) {
                result.setKeyEvents(objectMapper.readerForListOf(String.class).readValue(root.get("key_events")));
            }
            JsonNode m = root.path("token_metrics");
            result.setTokenMetrics(new TokenMetricsDto(m.path("sampled_frames").asInt(), m.path("raw_patch_tokens_per_frame").asInt(196), m.path("compressed_tokens_per_frame").asInt(5), m.path("raw_visual_tokens").asInt(), m.path("compressed_visual_tokens").asInt(), m.path("compression_ratio").asDouble(), root.path("runtime_metrics").path("total_ms").asLong()));
            result.setEstimatedLatencyMs(root.path("runtime_metrics").path("total_ms").asLong());
            result.setCreatedAt(TimeUtils.now());
            return result;
        } catch (Exception exception) {
            throw new BusinessException("RESULT_PARSE_FAILED", "结果解析失败：" + exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateHash(String taskId, Map<String, String> values) { redisTemplate.opsForHash().putAll(taskKey(taskId), values); }
    private String taskKey(String taskId) { return "task:" + taskId; }

    private InferenceTask toTask(Map<Object, Object> raw) throws Exception {
        InferenceTask task = new InferenceTask();
        task.setTaskId(str(raw, "taskId")); task.setVideoId(str(raw, "videoId")); task.setQueryText(str(raw, "queryText"));
        task.setStatus(TaskStatus.valueOf(str(raw, "status").toUpperCase())); task.setCurrentStage(str(raw, "currentStage")); task.setProgress(Integer.parseInt(raw.getOrDefault("progress", "0").toString()));
        task.setCreatedAt(Instant.parse(str(raw, "createdAt"))); task.setUpdatedAt(Instant.parse(str(raw, "updatedAt")));
        task.setErrorMessage((String) raw.get("errorMessage"));
        if (raw.get("summaryText") != null) task.appendSummaryText(raw.get("summaryText").toString());
        if (raw.get("tokenMetrics") != null) task.setTokenMetrics(objectMapper.readValue(raw.get("tokenMetrics").toString(), TokenMetricsDto.class));
        task.addLog(new TaskLog(IdGenerator.logId(), task.getTaskId(), "INFO", task.getCurrentStage(), "Redis mode task snapshot", TimeUtils.now()));
        return task;
    }
    private String str(Map<Object, Object> raw, String key) { Object value = raw.get(key); return value == null ? "" : value.toString(); }
}
