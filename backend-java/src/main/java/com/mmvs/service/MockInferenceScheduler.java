package com.mmvs.service;

import com.mmvs.config.InferenceProperties;
import com.mmvs.dto.StreamEvent;
import com.mmvs.dto.TokenMetricsDto;
import com.mmvs.model.InferenceResult;
import com.mmvs.model.InferenceTask;
import com.mmvs.model.TaskStatus;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
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
            sendStage(taskId, TaskStatus.WAITING, "waiting", 5, "任务已进入等待队列");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "video_sampling", 18, "正在按 1 FPS 进行视频抽帧");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "video_swin_feature", 35, "正在整理采样帧中的动作变化");
            sleep(inferenceProperties.getMockStageDelayMs());

            TokenMetricsDto metrics = buildTokenMetrics(taskId, System.currentTimeMillis() - started);
            taskService.setTokenMetrics(taskId, metrics);
            sseEmitterService.send(taskId, StreamEvent.tokenMetrics(taskId, "token_compression", metrics));
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "content_token", 52, "正在保留主体动作和关键事件");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "context_token", 66, "正在整理时间顺序和场景关联");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.RUNNING, "mlp_adapter", 78, "正在组织摘要所需的视觉线索");
            sleep(inferenceProperties.getMockStageDelayMs());

            sendStage(taskId, TaskStatus.STREAMING, "summary_generation", 86, "开始生成摘要文本");
            List<String> deltas = summaryDeltas(taskService.getTask(taskId));
            for (String delta : deltas) {
                sleep(inferenceProperties.getMockSummaryDelayMs());
                taskService.appendSummaryDelta(taskId, delta);
                sseEmitterService.send(taskId, StreamEvent.summaryDelta(taskId, delta));
            }

            long elapsed = System.currentTimeMillis() - started;
            InferenceResult result = taskService.completeTask(taskId, String.join("", deltas), keyEvents(), elapsed);
            sseEmitterService.send(taskId, StreamEvent.completed(taskId, result));
            sseEmitterService.complete(taskId);
        } catch (Exception exception) {
            taskService.failTask(taskId, "mock_inference", exception.getMessage());
            sseEmitterService.send(taskId, StreamEvent.error(taskId, "mock_inference", exception.getMessage()));
            sseEmitterService.complete(taskId);
        }
    }

    private void sendStage(String taskId, TaskStatus status, String stage, int progress, String message) {
        InferenceTask task = taskService.updateStage(taskId, status, stage, progress, message);
        sseEmitterService.send(taskId, StreamEvent.status(taskId, task.getStatus().getValue(), stage));
        sseEmitterService.send(taskId, StreamEvent.stage(taskId, task.getStatus().getValue(), stage));
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
        String query = task.getQueryText() == null ? "" : task.getQueryText();
        if (query.toLowerCase().contains("light")
                || query.toLowerCase().contains("switch")
                || query.toLowerCase().contains("lamp")
                || query.contains("开关")
                || query.contains("灯")) {
            return List.of(
                    "视频内容摘要：画面中可以观察到手部靠近墙面开关，并完成一次按压动作。",
                    "动作判断：按压动作集中发生在开关位置附近，核心事件较为单一。",
                    "亮度变化：当前结果未读取原始帧，不能明确判断开灯或关灯。",
                    "不确定性说明：请以 Python 轻量分析结果或原视频画面作为最终复核依据。"
            );
        }
        return List.of(
                "视频内容摘要：画面中存在连续场景变化，可按开头、主体动作和结果变化进行整理。",
                "动作判断：采样片段显示主体动作有明显推进，摘要优先保留可见事件。",
                "关键变化：结合用户指令“" + task.getQueryText() + "”，结果聚焦场景、动作和前后关系。",
                "不确定性说明：未能从当前链路确认的细节会保持保守表述。"
        );
    }

    private List<String> keyEvents() {
        return List.of(
                "读取视频文件。",
                "采样关键帧。",
                "整理主体动作和场景变化。",
                "生成中文摘要。"
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
