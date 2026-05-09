package com.mmvs.service;

import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.model.InferenceResult;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskStatus;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "in-memory", matchIfMissing = true)
public class MockInferenceScheduler {

    private final TaskService taskService;
    private final VideoStorageService videoStorageService;
    private final SseEmitterService sseEmitterService;
    private final InferenceProperties inferenceProperties;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public MockInferenceScheduler(
            TaskService taskService,
            VideoStorageService videoStorageService,
            SseEmitterService sseEmitterService,
            InferenceProperties inferenceProperties
    ) {
        this.taskService = taskService;
        this.videoStorageService = videoStorageService;
        this.sseEmitterService = sseEmitterService;
        this.inferenceProperties = inferenceProperties;
    }

    public void schedule(String taskId) {
        executorService.submit(() -> runMockPipeline(taskId));
    }

    private void runMockPipeline(String taskId) {
        long started = System.currentTimeMillis();
        try {
            sendStage(taskId, TaskStatus.WAITING, "waiting", 5, "任务已进入 in-memory mock 队列");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "video_sampling", 18, "正在按 1 FPS 进行视频抽帧");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "video_swin_feature", 35, "Video Swin Transformer 正在提取时空视觉特征");
            sleep(inferenceProperties.getMockStageDelayMs());

            TokenMetricsDto metrics = buildTokenMetrics(taskId, System.currentTimeMillis() - started);
            taskService.setTokenMetrics(taskId, metrics);
            sseEmitterService.send(taskId, StreamEvent.of(taskId, "token_metrics", "token_compression", metrics));
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "content_token", 52, "Content Token 分支保留主体动作和关键事件");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "context_token", 66, "Context Token 分支保留时间上下文和场景关联");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "mlp_adapter", 78, "MLP Projection Adapter 将视觉 Token 投影到文本语义空间");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.STREAMING, "summary_generation", 86, "LLM 摘要生成模块开始流式输出");
            List<String> deltas = summaryDeltas(taskService.getTask(taskId));
            for (String delta : deltas) {
                sleep(inferenceProperties.getMockSummaryDelayMs());
                taskService.appendSummaryDelta(taskId, delta);
                sseEmitterService.send(taskId, StreamEvent.of(taskId, "summary_delta", "summary_generation", Map.of("text", delta)));
            }

            long elapsed = System.currentTimeMillis() - started;
            InferenceResult result = taskService.completeTask(taskId, String.join("", deltas), keyEvents(), elapsed);
            sseEmitterService.send(taskId, StreamEvent.of(taskId, "completed", "finished", result));
            sseEmitterService.complete(taskId);
        } catch (Exception exception) {
            taskService.failTask(taskId, "mock_inference", exception.getMessage());
            sseEmitterService.send(taskId, StreamEvent.of(taskId, "error", "mock_inference", Map.of("message", exception.getMessage())));
            sseEmitterService.complete(taskId);
        }
    }

    private void sendStage(String taskId, TaskStatus status, String stage, int progress, String message) {
        InferenceTask task = taskService.updateStage(taskId, status, stage, progress, message);
        sseEmitterService.send(taskId, StreamEvent.of(taskId, "status", stage, Map.of(
                "status", task.getStatus().getValue(),
                "progress", task.getProgress()
        )));
        sseEmitterService.send(taskId, StreamEvent.of(taskId, "stage", stage, Map.of("message", message)));
    }

    private TokenMetricsDto buildTokenMetrics(String taskId, long elapsedMs) {
        InferenceTask task = taskService.getTask(taskId);
        long fileSize = videoStorageService.getRequiredVideo(task.getVideoId()).getFileSize();
        int estimatedDurationSeconds = (int) Math.max(8, Math.min(180, Math.ceil(fileSize / 700_000.0)));
        int sampledFrames = Math.max(1, estimatedDurationSeconds * inferenceProperties.getFrameSampleRate());
        int rawVisualTokens = sampledFrames * inferenceProperties.getRawPatchTokensPerFrame();
        int compressedVisualTokens = sampledFrames * inferenceProperties.getCompressedTokensPerFrame();
        double compressionRatio = (double) inferenceProperties.getRawPatchTokensPerFrame()
                / inferenceProperties.getCompressedTokensPerFrame();
        return new TokenMetricsDto(
                sampledFrames,
                inferenceProperties.getRawPatchTokensPerFrame(),
                inferenceProperties.getCompressedTokensPerFrame(),
                rawVisualTokens,
                compressedVisualTokens,
                compressionRatio,
                elapsedMs
        );
    }

    private List<String> summaryDeltas(InferenceTask task) {
        return List.of(
                "系统已完成视频抽帧，并从采样帧中识别出主要场景、主体动作和事件变化。",
                "Video Swin 特征提取阶段生成了时空视觉表示，为后续摘要提供动作与上下文依据。",
                "双轨 Token 压缩将单帧 196 个 Patch Token 压缩为 5 个视觉 Token，降低了后续生成阶段的输入长度。",
                "结合用户指令“" + task.getQueryText() + "”，系统生成了包含关键事件、场景变化和结果描述的中文摘要。",
                "本次任务为 in-memory mock mode，暂未调用 Python 推理服务，但保留 Redis 与真实模型接入边界。"
        );
    }

    private List<String> keyEvents() {
        return List.of(
                "视频抽帧：按 1 FPS 采样关键帧。",
                "特征提取：模拟 Video Swin Transformer 输出时空视觉特征。",
                "Token 压缩：Content Token 与 Context Token 共同完成 196 → 5 压缩。",
                "摘要生成：以 SSE 风格逐句输出中文摘要。"
        );
    }

    private void sleep(long millis) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(millis);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}
