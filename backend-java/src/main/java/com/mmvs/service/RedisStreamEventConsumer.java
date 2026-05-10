package com.mmvs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.model.TaskStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisStreamEventConsumer {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final InferenceProperties inferenceProperties;
    private final TaskService taskService;
    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;
    private String lastSeenId = "0-0";

    public RedisStreamEventConsumer(
            StringRedisTemplate redisTemplate,
            InferenceProperties inferenceProperties,
            TaskService taskService,
            SseEmitterService sseEmitterService,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.inferenceProperties = inferenceProperties;
        this.taskService = taskService;
        this.sseEmitterService = sseEmitterService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        if (inferenceProperties.isRedisMode()) {
            executorService.submit(this::consumeLoop);
        }
    }

    private void consumeLoop() {
        while (running) {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                            .block(Duration.ofSeconds(2))
                            .count(10),
                    StreamOffset.create(inferenceProperties.getRedisEventStream(), ReadOffset.from(lastSeenId))
            );
            if (records == null) {
                continue;
            }
            for (MapRecord<String, Object, Object> record : records) {
                lastSeenId = record.getId().getValue();
                handleRecord(record.getValue());
            }
        }
    }

    private void handleRecord(Map<Object, Object> value) {
        String taskId = field(value, "taskId");
        String eventType = field(value, "eventType");
        String status = field(value, "status");
        String stage = field(value, "stage");
        if (taskId.isBlank() || eventType.isBlank()) {
            return;
        }

        StreamEvent event = new StreamEvent(
                taskId,
                eventType,
                blankToNull(status),
                blankToNull(stage),
                tokenMetrics(value),
                blankToNull(field(value, "summaryDelta")),
                parseJsonObject(field(value, "completed")),
                blankToNull(field(value, "error")),
                java.time.Instant.now()
        );
        applyToTask(event);
        sseEmitterService.send(taskId, event);
        if ("completed".equals(event.eventType()) || "error".equals(event.eventType())) {
            sseEmitterService.complete(taskId);
        }
    }

    private void applyToTask(StreamEvent event) {
        switch (event.eventType()) {
            case "status", "stage" -> taskService.updateStage(
                    event.taskId(),
                    parseStatus(event.status()),
                    event.stage() == null ? "running" : event.stage(),
                    progressFor(event.stage()),
                    "Redis event: " + event.eventType()
            );
            case "token_metrics" -> {
                if (event.tokenMetrics() != null) {
                    taskService.setTokenMetrics(event.taskId(), event.tokenMetrics());
                }
            }
            case "summary_delta" -> {
                if (event.summaryDelta() != null) {
                    taskService.appendSummaryDelta(event.taskId(), event.summaryDelta());
                }
            }
            case "completed" -> completeTask(event);
            case "error" -> taskService.failTask(
                    event.taskId(),
                    event.stage() == null ? "redis_worker" : event.stage(),
                    event.error() == null ? "Python worker failed" : event.error()
            );
            default -> {
                // 保留未知事件转发能力。
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void completeTask(StreamEvent event) {
        Map<String, Object> completed = event.completed() instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Collections.emptyMap();
        String summary = stringValue(completed.getOrDefault("summary", ""));
        List<String> keyEvents = completed.get("keyEvents") instanceof List<?> list
                ? list.stream().map(this::stringValue).toList()
                : List.of();
        long latency = longValue(completed.get("estimatedLatencyMs"));
        taskService.completeTask(event.taskId(), summary, keyEvents, latency);
    }

    private TokenMetricsDto tokenMetrics(Map<Object, Object> value) {
        Map<String, Object> metrics = parseJsonObject(field(value, "tokenMetrics"));
        if (metrics.isEmpty()) {
            return null;
        }
        return new TokenMetricsDto(
                intValue(metrics.get("sampledFrames")),
                intValue(metrics.get("rawPatchTokensPerFrame")),
                intValue(metrics.get("compressedTokensPerFrame")),
                intValue(metrics.get("rawVisualTokens")),
                intValue(metrics.get("compressedVisualTokens")),
                doubleValue(metrics.get("compressionRatio")),
                longValue(metrics.get("estimatedLatencyMs"))
        );
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private TaskStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return TaskStatus.RUNNING;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "waiting" -> TaskStatus.WAITING;
            case "streaming" -> TaskStatus.STREAMING;
            case "finished" -> TaskStatus.FINISHED;
            case "failed" -> TaskStatus.FAILED;
            case "cancelled" -> TaskStatus.CANCELLED;
            default -> TaskStatus.RUNNING;
        };
    }

    private int progressFor(String stage) {
        if (stage == null) {
            return 10;
        }
        return switch (stage) {
            case "waiting" -> 5;
            case "video_preprocess", "video_sampling" -> 18;
            case "video_swin", "video_swin_feature" -> 35;
            case "token_compression", "content_token" -> 52;
            case "context_token" -> 66;
            case "projection_adapter", "mlp_adapter" -> 78;
            case "summary_generation" -> 86;
            case "finished" -> 100;
            default -> 45;
        };
    }

    private String field(Map<Object, Object> value, String key) {
        Object raw = value.get(key);
        return raw == null ? "" : raw.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(stringValue(value));
    }

    private long longValue(Object value) {
        if (value == null || stringValue(value).isBlank()) return 0L;
        return value instanceof Number number ? number.longValue() : Long.parseLong(stringValue(value));
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(stringValue(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    @PreDestroy
    public void stop() {
        running = false;
        executorService.shutdownNow();
    }
}
