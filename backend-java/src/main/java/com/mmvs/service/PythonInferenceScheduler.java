package com.mmvs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.model.InferenceResult;
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
import java.util.LinkedHashMap;
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
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

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

    private void runPythonPipeline(String taskId) {
        long started = System.currentTimeMillis();
        try {
            sendStage(taskId, TaskStatus.WAITING, "waiting", 5, "任务已进入分析队列");
            sendStage(taskId, TaskStatus.RUNNING, "video_sampling", 18, "正在读取视频并采样关键帧");

            InferenceTask task = taskService.getTask(taskId);
            VideoFile videoFile = videoStorageService.getRequiredVideo(task.getVideoId());
            JsonNode response = requestPython(task, videoFile);

            TokenMetricsDto metrics = tokenMetrics(response.path("token_metrics"), System.currentTimeMillis() - started);
            taskService.setTokenMetrics(taskId, metrics);
            sseEmitterService.send(taskId, StreamEvent.tokenMetrics(taskId, "token_compression", metrics));

            sendStage(taskId, TaskStatus.RUNNING, "summary_generation", 86, "正在生成分析结果");
            List<String> chunks = textList(response.path("summary_chunks"));
            if (chunks.isEmpty()) {
                chunks = List.of(textValue(response.path("summary"), "未返回摘要内容。"));
            }
            for (String chunk : chunks) {
                taskService.appendSummaryDelta(taskId, chunk);
                sseEmitterService.send(taskId, StreamEvent.summaryDelta(taskId, chunk));
            }

            List<String> keyEvents = textList(response.path("key_events"));
            long elapsed = runtimeTotal(response.path("runtime_metrics"), System.currentTimeMillis() - started);
            InferenceResult result = taskService.completeTask(taskId, textValue(response.path("summary"), String.join("", chunks)), keyEvents, elapsed);
            sseEmitterService.send(taskId, StreamEvent.completed(taskId, completedPayload(response, result)));
            sseEmitterService.complete(taskId);
        } catch (Exception exception) {
            String message = "Python 分析服务未连接或返回异常，请先运行一键启动脚本或检查 http://localhost:8000/health。";
            if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
                message = message + " 详情：" + exception.getMessage();
            }
            taskService.failTask(taskId, "python_analysis", message);
            sseEmitterService.send(taskId, StreamEvent.error(taskId, "python_analysis", message));
            sseEmitterService.complete(taskId);
        }
    }

    private JsonNode requestPython(InferenceTask task, VideoFile videoFile) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.getTaskId());
        payload.put("videoPath", videoFile.getStoredPath());
        payload.put("queryText", task.getQueryText());
        payload.put("scenarioType", lightSwitchRequested(task.getQueryText(), videoFile.getOriginalFileName()) ? "light_switch_demo" : "");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(inferenceProperties.getPythonBaseUrl().replaceAll("/$", "") + "/infer"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Python 服务返回 HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private boolean lightSwitchRequested(String queryText, String fileName) {
        String text = ((queryText == null ? "" : queryText) + " " + (fileName == null ? "" : fileName)).toLowerCase();
        return text.contains("light") || text.contains("switch") || text.contains("lamp") || text.contains("开关") || text.contains("灯");
    }

    private void sendStage(String taskId, TaskStatus status, String stage, int progress, String message) {
        InferenceTask task = taskService.updateStage(taskId, status, stage, progress, message);
        sseEmitterService.send(taskId, StreamEvent.status(taskId, task.getStatus().getValue(), stage));
        sseEmitterService.send(taskId, StreamEvent.stage(taskId, task.getStatus().getValue(), stage));
    }

    private TokenMetricsDto tokenMetrics(JsonNode node, long elapsedMs) {
        if (node == null || node.isMissingNode()) {
            return new TokenMetricsDto(0, 196, 5, 0, 0, 196.0 / 5.0, elapsedMs);
        }
        return new TokenMetricsDto(
                intValue(node.path("sampled_frames"), node.path("sampledFrames"), 0),
                intValue(node.path("raw_patch_tokens_per_frame"), node.path("rawPatchTokensPerFrame"), 196),
                intValue(node.path("compressed_tokens_per_frame"), node.path("compressedTokensPerFrame"), 5),
                intValue(node.path("raw_visual_tokens"), node.path("rawVisualTokens"), 0),
                intValue(node.path("compressed_visual_tokens"), node.path("compressedVisualTokens"), 0),
                doubleValue(node.path("compression_ratio"), node.path("compressionRatio"), 196.0 / 5.0),
                elapsedMs
        );
    }

    private Map<String, Object> completedPayload(JsonNode response, InferenceResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", result.getSummary());
        payload.put("keyEvents", result.getKeyEvents());
        payload.put("estimatedLatencyMs", result.getEstimatedLatencyMs());
        JsonNode analysis = response.path("light_switch_analysis");
        if (!analysis.isMissingNode() && !analysis.isNull()) {
            payload.put("scenarioType", "light_switch_demo");
            payload.put("lightSwitchAnalysis", objectMapper.convertValue(analysis, Map.class));
        }
        JsonNode fallbackReason = response.path("model_info").path("fallbackReason");
        if (!fallbackReason.isMissingNode() && !fallbackReason.asText().isBlank()) {
            payload.put("fallbackReason", fallbackReason.asText());
        }
        return payload;
    }

    private List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                values.add(item.asText());
            }
        }
        return values;
    }

    private long runtimeTotal(JsonNode node, long fallback) {
        return Math.round(doubleValue(node.path("total_ms"), node.path("totalMs"), fallback));
    }

    private String textValue(JsonNode node, String fallback) {
        return node == null || node.isMissingNode() || node.isNull() ? fallback : node.asText(fallback);
    }

    private int intValue(JsonNode snake, JsonNode camel, int fallback) {
        JsonNode selected = snake.isMissingNode() ? camel : snake;
        return selected.isNumber() ? selected.asInt() : fallback;
    }

    private double doubleValue(JsonNode snake, JsonNode camel, double fallback) {
        JsonNode selected = snake.isMissingNode() ? camel : snake;
        return selected.isNumber() ? selected.asDouble() : fallback;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}
