import { computed, ref } from 'vue';
import { connectTaskEvents, type TaskEventClient } from '../api/sseClient';
import { createTask } from '../api/taskApi';
import { createInitialStages } from '../data/sampleTasks';
import type { CreateTaskInput, InferenceTask, PipelineStage, PipelineStageKey, TaskStatus } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';
import { useTaskStream } from './useTaskStream';

interface BackendTaskResponse {
  taskId: string;
  videoId: string;
  queryText: string;
  status: TaskStatus;
  currentStage: string;
  progress: number;
  summaryText?: string;
  tokenMetrics?: {
    sampledFrames: number;
    rawPatchTokensPerFrame: number;
    compressedTokensPerFrame: number;
    rawVisualTokens: number;
    compressedVisualTokens: number;
    compressionRatio: number;
    estimatedLatencyMs: number;
  };
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
  finishedAt?: string;
}

interface LightSwitchAnalysis {
  scenarioType?: string;
  motionDetected?: boolean;
  brightnessTrend?: 'brighter' | 'darker' | 'unchanged';
  operationGuess?: 'turn_on' | 'turn_off' | 'press_only' | 'unknown';
  brightnessDelta?: number;
  confidence?: number;
}

interface BackendCompletedPayload {
  summary?: string;
  keyEvents?: string[];
  estimatedLatencyMs?: number;
  scenarioType?: string;
  lightSwitchAnalysis?: LightSwitchAnalysis;
  fallbackReason?: string;
}

interface BackendStreamEvent {
  taskId: string;
  eventType: string;
  status?: TaskStatus;
  stage?: string;
  tokenMetrics?: BackendTaskResponse['tokenMetrics'];
  summaryDelta?: string;
  completed?: BackendCompletedPayload;
  error?: string;
}

function toStage(stage?: string): PipelineStageKey {
  if (stage === 'video_preprocess' || stage === 'video_sampling') return 'video_sampling';
  if (stage === 'video_swin' || stage === 'video_swin_feature') return 'video_swin';
  if (stage === 'token_compression' || stage === 'content_token') return 'content_token';
  if (stage === 'projection_adapter' || stage === 'mlp_adapter') return 'mlp_adapter';
  if (stage === 'summary_generation') return 'llm_generation';
  if (stage === 'finished') return 'finished';
  return 'waiting';
}

function updateStages(stages: PipelineStage[], currentStage: PipelineStageKey): PipelineStage[] {
  const stageOrder = stages.findIndex((stage) => stage.key === currentStage);
  return stages.map((stage, index) => {
    if (index < stageOrder) return { ...stage, status: 'done' };
    if (stage.key === currentStage) return { ...stage, status: 'active', detail: '分析服务正在返回实时进度。' };
    return { ...stage, status: 'pending' };
  });
}

function lightSwitchConclusion(completed: BackendCompletedPayload): string {
  if (completed.fallbackReason) {
    return '已回退到常规摘要流程，轻量视觉分析未运行。';
  }
  if (completed.scenarioType !== 'light_switch_demo' || !completed.lightSwitchAnalysis) {
    return '正式分析已完成。';
  }
  const analysis = completed.lightSwitchAnalysis;
  if (analysis.operationGuess === 'turn_on') return '检测到开关按压动作，亮度上升判断为谨慎推测。';
  if (analysis.operationGuess === 'turn_off') return '检测到开关按压动作，亮度下降判断为谨慎推测。';
  if (analysis.motionDetected) return '检测到开关按压动作，但亮度变化不足以判断开灯或关灯。';
  return '画面证据较弱，需结合原视频复核。';
}

function metricsFromBackend(metrics: BackendTaskResponse['tokenMetrics'], durationSeconds: number) {
  const base = createTokenMetrics(durationSeconds);
  if (!metrics) return base;
  return {
    ...base,
    sampledFrames: metrics.sampledFrames,
    rawPatchTokensPerFrame: metrics.rawPatchTokensPerFrame,
    compressedTokensPerFrame: metrics.compressedTokensPerFrame,
    rawTotalTokens: metrics.rawVisualTokens,
    compressedTotalTokens: metrics.compressedVisualTokens,
    compressionRatio: metrics.compressionRatio,
  };
}

function createBackendTask(input: CreateTaskInput, response: BackendTaskResponse): InferenceTask {
  return {
    taskId: response.taskId,
    video: { ...input.video, videoId: response.videoId },
    instruction: response.queryText,
    status: response.status,
    currentStage: toStage(response.currentStage),
    progress: response.progress,
    stages: updateStages(createInitialStages(), toStage(response.currentStage)),
    streamChunks: [],
    tokenMetrics: metricsFromBackend(response.tokenMetrics, input.video.durationSeconds),
    runtimeMetrics: { preprocessMs: 0, featureExtractMs: 0, tokenCompressMs: 0, adapterMs: 0, generationMs: 0, totalMs: 0 },
    createdAt: response.createdAt,
    updatedAt: response.updatedAt,
    finishedAt: response.finishedAt,
    errorMessage: response.errorMessage,
  };
}

export function useBackendInferenceTask(onFinished: (task: InferenceTask) => void) {
  const currentTask = ref<InferenceTask | null>(null);
  const errorMessage = ref('');
  const { isStreaming, streamError, startStream, stopStream, markStreamError } = useTaskStream();
  let client: TaskEventClient | null = null;

  const canCreateTask = computed(() => !isStreaming.value && currentTask.value?.status !== 'running' && currentTask.value?.status !== 'streaming');

  async function createAndRunTask(input: CreateTaskInput) {
    if (!input.instruction.trim()) {
      errorMessage.value = '请输入摘要或问答指令。';
      return;
    }
    errorMessage.value = '';
    startStream();
    client?.close();

    try {
      const response = await createTask({ videoId: input.video.videoId, instruction: input.instruction, runMode: 'real', stream: true });
      currentTask.value = createBackendTask(input, response.data as unknown as BackendTaskResponse);
      client = connectTaskEvents(currentTask.value.taskId, handleEvent, () => markStreamError('连接已断开，请检查服务状态。'));
    } catch (error) {
      const message = error instanceof Error ? error.message : '创建分析任务失败。';
      markStreamError(message);
      errorMessage.value = message;
    }
  }

  function handleEvent(_eventName: string, payload: unknown) {
    if (!currentTask.value) return;
    const event = payload as BackendStreamEvent;
    const updatedAt = new Date().toISOString();

    if (event.status || event.stage) {
      const nextStage = toStage(event.stage);
      currentTask.value = {
        ...currentTask.value,
        status: event.status ?? currentTask.value.status,
        currentStage: nextStage,
        progress: event.stage === 'finished' ? 100 : Math.max(currentTask.value.progress, 10),
        stages: updateStages(currentTask.value.stages, nextStage),
        updatedAt,
      };
    }

    if (event.tokenMetrics) {
      currentTask.value = { ...currentTask.value, tokenMetrics: metricsFromBackend(event.tokenMetrics, currentTask.value.video.durationSeconds), updatedAt };
    }

    if (event.summaryDelta) {
      currentTask.value = {
        ...currentTask.value,
        status: 'streaming',
        streamChunks: [...currentTask.value.streamChunks, { id: `chunk-${crypto.randomUUID()}`, text: event.summaryDelta, createdAt: updatedAt }],
        progress: Math.min(96, currentTask.value.progress + 3),
        updatedAt,
      };
    }

    if (event.completed) {
      const finishedAt = new Date().toISOString();
      currentTask.value = {
        ...currentTask.value,
        status: 'finished',
        currentStage: 'finished',
        progress: 100,
        stages: currentTask.value.stages.map((stage) => ({ ...stage, status: 'done' })),
        runtimeMetrics: { ...currentTask.value.runtimeMetrics, totalMs: event.completed.estimatedLatencyMs ?? 0 },
        result: { summary: event.completed.summary ?? '', keyEvents: event.completed.keyEvents ?? [], conclusion: lightSwitchConclusion(event.completed) },
        updatedAt: finishedAt,
        finishedAt,
      };
      client?.close();
      stopStream();
      onFinished(currentTask.value);
    }

    if (event.error) {
      currentTask.value = { ...currentTask.value, status: 'failed', errorMessage: event.error, updatedAt };
      errorMessage.value = event.error;
      client?.close();
      markStreamError(event.error);
    }
  }

  return { currentTask, errorMessage, isStreaming, streamError, canCreateTask, createAndRunTask };
}
