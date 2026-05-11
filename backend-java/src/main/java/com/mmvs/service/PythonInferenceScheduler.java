package com.mmvs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskStatus;
import com.mmvs.model.VideoFile;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

@Service
public class PythonInferenceScheduler {

    private final TaskService taskService;
    private final VideoStorageService videoStorageService;
    private final SseEmitterService sseEmitterService;
    private final InferenceProperties inferenceProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public PythonInferenceScheduler(
            TaskService taskService,
            VideoStorageService videoStorageService,
            SseEmitterService sseEmitterService,
            InferenceProperties inferenceProperties,
            ObjectMapper objectMapper
    ) {
        this.taskService = taskService;
        this.videoStorageService = videoStorageService;
        this.sseEmitterService = sseEmitterService;
        this.inferenceProperties = inferenceProperties;
        this.objectMapper = objectMapper;
    }

    public void schedule(String taskId) {
        executorService.submit(() -> runPythonPipeline(taskId));
    }

    public boolean isPythonHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl() + "/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void runPythonPipeline(String taskId) {
        try {
            sendStage(taskId, TaskStatus.WAITING, "waiting", 5, "任务已创建，等待开始分析");
            InferenceTask task = taskService.getTask(taskId);
            VideoFile videoFile = videoStorageService.getRequiredVideo(task.getVideoId());
            sendStage(taskId, TaskStatus.RUNNING, "video_sampling", 18, "正在读取视频并采样关键帧");

            JsonNode result = callPython(task, videoFile);
            TokenMetricsDto metrics = tokenMetrics(result.path("token_metrics"));
            taskService.setTokenMetrics(taskId, metrics);
            sseEmitterService.send(taskId, StreamEvent.tokenMetrics(taskId, "token_compression", metrics));

            sendStage(taskId, TaskStatus.RUNNING, "content_token", 52, "正在整理动作、亮度变化和关键事件");
            sendStage(taskId, TaskStatus.STREAMING, "summary_generation", 86, "正在输出摘要结果");
            for (String chunk : summaryChunks(result)) {
                taskService.appendSummaryDelta(taskId, chunk);
                sseEmitterService.send(taskId, StreamEvent.summaryDelta(taskId, chunk));
            }

            Map<String, Object> completed = completedPayload(result);
            String summary = text(result, "summary", String.join("", summaryChunks(result)));
            List<String> keyEvents = keyEvents(result);
            long latency = result.path("runtime_metrics").path("total_ms").asLong(0L);
            taskService.completeTask(taskId, summary, keyEvents, latency);
            sseEmitterService.send(taskId, StreamEvent.completed(taskId, completed));
            sseEmitterService.complete(taskId);
        } catch (Exception exception) {
            String message = "正式分析失败：" + exception.getMessage();
            taskService.failTask(taskId, "analysis", message);
            sseEmitterService.send(taskId, StreamEvent.error(taskId, "analysis", message));
            sseEmitterService.complete(taskId);
        }
    }

    private JsonNode callPython(InferenceTask task, VideoFile videoFile) throws Exception {
        Map<String, Object> payload = Map.of(
                "taskId", task.getTaskId(),
                "videoPath", videoFile.getStoredPath(),
                "queryText", task.getQueryText(),
                "scenarioType", "light_switch_demo"
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/infer"))
                .timeout(Duration.ofSeconds(inferenceProperties.getPythonTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("分析服务返回 " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private void sendStage(String taskId, TaskStatus status, String stage, int progress, String detail) {
        taskService.updateStage(taskId, status, stage, progress, detail);
        sseEmitterService.send(taskId, StreamEvent.stage(taskId, status.getValue(), stage));
    }

    private TokenMetricsDto tokenMetrics(JsonNode node) {
        return new TokenMetricsDto(
                node.path("sampled_frames").asInt(1),
                node.path("raw_patch_tokens_per_frame").asInt(196),
                node.path("compressed_tokens_per_frame").asInt(5),
                node.path("raw_visual_tokens").asInt(196),
                node.path("compressed_visual_tokens").asInt(5),
                node.path("compression_ratio").asDouble(39.2),
                0L
        );
    }

    private List<String> summaryChunks(JsonNode result) {
        List<String> chunks = new ArrayList<>();
        JsonNode node = result.path("summary_chunks");
        if (node.isArray()) {
            node.forEach(item -> chunks.add(item.asText()));
        }
        if (chunks.isEmpty()) {
            chunks.add(text(result, "summary", "正式分析已完成，但未返回摘要文本。"));
        }
        return chunks;
    }

    private List<String> keyEvents(JsonNode result) {
        List<String> events = new ArrayList<>();
        JsonNode node = result.path("key_events");
        if (node.isArray()) {
            node.forEach(item -> events.add(item.asText()));
        }
        return events;
    }

    private Map<String, Object> completedPayload(JsonNode result) {
        Map<String, Object> completed = new HashMap<>();
        completed.put("summary", text(result, "summary", ""));
        completed.put("keyEvents", keyEvents(result));
        completed.put("estimatedLatencyMs", result.path("runtime_metrics").path("total_ms").asLong(0L));
        JsonNode analysis = result.path("light_switch_analysis");
        if (!analysis.isMissingNode() && !analysis.isNull()) {
            completed.put("scenarioType", "light_switch_demo");
            completed.put("lightSwitchAnalysis", objectMapper.convertValue(analysis, Map.class));
        }
        JsonNode fallbackReason = result.path("model_info").path("fallbackReason");
        if (!fallbackReason.isMissingNode() && !fallbackReason.asText().isBlank()) {
            completed.put("fallbackReason", fallbackReason.asText());
        }
        return completed;
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private String baseUrl() {
        return inferenceProperties.getPythonBaseUrl().replaceAll("/+$", "");
    }

    @PreDestroy
    public void stop() {
        executorService.shutdownNow();
    }
}
