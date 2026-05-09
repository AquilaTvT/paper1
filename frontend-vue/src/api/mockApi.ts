import { keyEventTemplates, summaryTemplates } from '../data/sampleSummaries';
import { createInitialStages } from '../data/sampleTasks';
import type { CreateTaskInput, InferenceTask, PipelineStageKey, SummaryResult, VideoFileInfo } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';

interface MockStageEvent {
  type: 'stage';
  stage: PipelineStageKey;
  status: InferenceTask['status'];
  progress: number;
  detail: string;
}

interface MockDeltaEvent {
  type: 'delta';
  text: string;
}

interface MockDoneEvent {
  type: 'done';
  result: SummaryResult;
}

export type MockInferenceEvent = MockStageEvent | MockDeltaEvent | MockDoneEvent;

const wait = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

export function createVideoFromFile(file: File): VideoFileInfo {
  return {
    videoId: `video-${crypto.randomUUID()}`,
    name: file.name,
    sizeBytes: file.size,
    // 浏览器不可靠读取视频时长时，使用适合演示的估算值，用户仍可通过样例视频验证指标变化。
    durationSeconds: Math.max(12, Math.min(180, Math.round(file.size / 560_000))),
    source: 'upload',
    objectUrl: URL.createObjectURL(file),
    createdAt: new Date().toISOString(),
  };
}

export function createMockTask(input: CreateTaskInput): InferenceTask {
  const now = new Date().toISOString();

  return {
    taskId: `task-${crypto.randomUUID()}`,
    video: input.video,
    instruction: input.instruction,
    status: 'waiting',
    currentStage: 'waiting',
    progress: 5,
    stages: createInitialStages(),
    streamChunks: [],
    tokenMetrics: createTokenMetrics(input.video.durationSeconds),
    runtimeMetrics: {
      preprocessMs: 0,
      featureExtractMs: 0,
      tokenCompressMs: 0,
      adapterMs: 0,
      generationMs: 0,
      totalMs: 0,
    },
    createdAt: now,
    updatedAt: now,
  };
}

export async function runMockInference(onEvent: (event: MockInferenceEvent) => void): Promise<void> {
  const stageEvents: MockStageEvent[] = [
    {
      type: 'stage',
      stage: 'waiting',
      status: 'waiting',
      progress: 8,
      detail: '任务已写入 Redis 队列，等待 Python worker 消费。',
    },
    {
      type: 'stage',
      stage: 'video_sampling',
      status: 'running',
      progress: 18,
      detail: '正在按 1 FPS 执行视频抽帧，构建采样帧序列。',
    },
    {
      type: 'stage',
      stage: 'video_swin',
      status: 'running',
      progress: 35,
      detail: 'Video Swin Transformer mock 特征提取完成，单帧原始 Patch Token 为 196。',
    },
    {
      type: 'stage',
      stage: 'content_token',
      status: 'running',
      progress: 50,
      detail: 'Content Token 分支保留主体动作、关键事件和视觉显著区域。',
    },
    {
      type: 'stage',
      stage: 'context_token',
      status: 'running',
      progress: 62,
      detail: 'Context Token 分支保留时间上下文、场景关联和用户指令相关信息。',
    },
    {
      type: 'stage',
      stage: 'mlp_adapter',
      status: 'running',
      progress: 74,
      detail: 'MLP Projection Adapter 将 5 个视觉 Token 投影到摘要生成语义空间。',
    },
    {
      type: 'stage',
      stage: 'llm_generation',
      status: 'streaming',
      progress: 84,
      detail: 'LLM 生成模块开始以 SSE 风格逐句输出摘要。',
    },
  ];

  for (const event of stageEvents) {
    await wait(520);
    onEvent(event);
  }

  for (const text of summaryTemplates) {
    await wait(700);
    onEvent({ type: 'delta', text });
  }

  await wait(460);
  onEvent({
    type: 'done',
    result: {
      summary: summaryTemplates.join(''),
      keyEvents: keyEventTemplates,
      conclusion: 'mock mode 已完成完整演示链路，结果可写入 localStorage 并用于论文截图。',
    },
  });
}
