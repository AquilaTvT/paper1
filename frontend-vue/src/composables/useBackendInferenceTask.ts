import { computed, ref } from 'vue';
import { connectTaskEvents, type TaskEventClient } from '../api/sseClient';
import { createTask } from '../api/taskApi';
import { createInitialStages, sampleVideo } from '../data/sampleTasks';
import type { InferenceTask, PipelineStageKey, VideoFileInfo } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';

type BackendPayload = { payload?: Record<string, unknown>; eventType?: string; stage?: string; createdAt?: string } & Record<string, unknown>;

export function useBackendInferenceTask(onFinished: (task: InferenceTask) => void) {
  const currentTask = ref<InferenceTask | null>(null);
  const errorMessage = ref('');
  const streamError = ref('');
  const isStreaming = ref(false);
  let client: TaskEventClient | null = null;

  const canCreateTask = computed(() => !isStreaming.value);

  async function createAndRunTask(input: { video: VideoFileInfo; instruction: string }) {
    errorMessage.value = '';
    streamError.value = '';
    const response = await createTask({ videoId: input.video.videoId, queryText: input.instruction, instruction: input.instruction, runMode: 'mock', stream: true });
    const raw = response.data as unknown as Record<string, unknown>;
    const taskId = String(raw.taskId);
    currentTask.value = baseTask(taskId, input.video, input.instruction, String(raw.status || 'waiting'));
    isStreaming.value = true;
    client?.close();
    client = connectTaskEvents(taskId, handleEvent, () => {
      streamError.value = 'SSE 连接已断开，可重新创建任务或刷新页面重连。';
      isStreaming.value = false;
    });
  }

  function handleEvent(eventName: string, data: unknown) {
    if (!currentTask.value) return;
    const event = data as BackendPayload;
    const payload = (event.payload || event) as Record<string, unknown>;
    const now = new Date().toISOString();
    if (eventName === 'status') {
      const status = String(payload.status || 'running') as InferenceTask['status'];
      currentTask.value = { ...currentTask.value, status, progress: Number(payload.progress || currentTask.value.progress), updatedAt: now };
    }
    if (eventName === 'stage') {
      currentTask.value = { ...currentTask.value, currentStage: mapStage(String(event.stage || payload.stage || 'video_sampling')), updatedAt: now };
    }
    if (eventName === 'token_metrics') {
      currentTask.value = { ...currentTask.value, tokenMetrics: createTokenMetrics(currentTask.value.video.durationSeconds), updatedAt: now };
    }
    if (eventName === 'summary_delta') {
      const text = String(payload.text || '');
      currentTask.value = { ...currentTask.value, status: 'streaming', streamChunks: [...currentTask.value.streamChunks, { id: crypto.randomUUID(), text, createdAt: now }], updatedAt: now };
    }
    if (eventName === 'completed') {
      currentTask.value = { ...currentTask.value, status: 'finished', currentStage: 'finished', progress: 100, finishedAt: now, updatedAt: now };
      isStreaming.value = false;
      client?.close();
      onFinished(currentTask.value);
    }
    if (eventName === 'error') {
      errorMessage.value = String(payload.message || '后端推理失败');
      currentTask.value = { ...currentTask.value, status: 'failed', errorMessage: errorMessage.value, updatedAt: now };
      isStreaming.value = false;
    }
  }

  function baseTask(taskId: string, video: VideoFileInfo, instruction: string, status: string): InferenceTask {
    return { taskId, video, instruction, status: status as InferenceTask['status'], currentStage: 'waiting', progress: 0, stages: createInitialStages(), streamChunks: [], tokenMetrics: createTokenMetrics(video.durationSeconds || sampleVideo.durationSeconds), runtimeMetrics: { preprocessMs: 0, featureExtractMs: 0, tokenCompressMs: 0, adapterMs: 0, generationMs: 0, totalMs: 0 }, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() };
  }

  function mapStage(stage: string): PipelineStageKey {
    if (stage.includes('swin')) return 'video_swin';
    if (stage.includes('content')) return 'content_token';
    if (stage.includes('context')) return 'context_token';
    if (stage.includes('adapter')) return 'mlp_adapter';
    if (stage.includes('summary')) return 'llm_generation';
    if (stage.includes('finish')) return 'finished';
    return 'video_sampling';
  }

  return { currentTask, errorMessage, isStreaming, streamError, canCreateTask, createAndRunTask };
}
