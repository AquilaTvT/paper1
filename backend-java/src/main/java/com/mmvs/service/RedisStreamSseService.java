package com.mmvs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmvs.dto.StreamEvent;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamReadOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "redis")
public class RedisStreamSseService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public RedisStreamSseService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(String taskId) {
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        executor.submit(() -> readLoop(taskId, emitter));
        return emitter;
    }

    private void readLoop(String taskId, SseEmitter emitter) {
        String streamKey = "stream:task:" + taskId;
        ReadOffset offset = ReadOffset.from("0-0");
        try {
            while (true) {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        StreamReadOptions.empty().block(Duration.ofSeconds(2)).count(10),
                        StreamOffset.create(streamKey, offset)
                );
                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (MapRecord<String, Object, Object> record : records) {
                    offset = ReadOffset.from(record.getId().getValue());
                    Map<Object, Object> value = record.getValue();
                    String eventType = String.valueOf(value.getOrDefault("eventType", "stage"));
                    String stage = String.valueOf(value.getOrDefault("stage", eventType));
                    Object payload = parsePayload(String.valueOf(value.getOrDefault("payload", "{}")));
                    if ("summary_delta".equals(eventType) && value.get("summaryDelta") != null) {
                        payload = Map.of("summaryDelta", value.get("summaryDelta"), "text", value.get("summaryDelta"));
                    }
                    if ("token_metrics".equals(eventType) && value.get("tokenMetrics") != null) {
                        payload = parsePayload(String.valueOf(value.get("tokenMetrics")));
                    }
                    if ("status".equals(eventType) && value.get("status") != null) {
                        payload = Map.of("status", value.get("status"));
                    }
                    if ("error".equals(eventType) && value.get("error") != null) {
                        payload = Map.of("error", value.get("error"), "message", value.get("error"));
                    }
                    emitter.send(SseEmitter.event().name(eventType).data(StreamEvent.of(taskId, eventType, stage, payload)));
                    if ("completed".equals(eventType) || "error".equals(eventType)) {
                        emitter.complete();
                        return;
                    }
                }
            }
        } catch (Exception exception) {
            try { emitter.completeWithError(exception); } catch (Exception ignored) { }
        }
    }

    private Object parsePayload(String payload) {
        try { return objectMapper.readValue(payload, Object.class); } catch (Exception ignored) { return Map.of("text", payload); }
    }
}
