import { selectScenario, type SampleVideoScenario } from '../data/sampleVideoScenarios';
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

export interface CreateVideoOptions {
  durationSeconds?: number;
  durationReadable?: boolean;
  fileType?: string;
}

export type MockInferenceEvent = MockStageEvent | MockDeltaEvent | MockDoneEvent;

const wait = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

function fallbackDuration(file: File): number {
  return Math.max(12, Math.min(180, Math.round(file.size / 560_000)));
}

export function createVideoFromFile(file: File, options: CreateVideoOptions = {}): VideoFileInfo {
  return {
    videoId: `video-${crypto.randomUUID()}`,
    name: file.name,
    sizeBytes: file.size,
    fileType: options.fileType ?? file.type ?? '未知格式',
    durationSeconds: options.durationSeconds ?? fallbackDuration(file),
    durationReadable: options.durationReadable ?? false,
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

function buildSummaryLines(input: CreateTaskInput, scenario: SampleVideoScenario): string[] {
  const metrics = createTokenMetrics(input.video.durationSeconds);
  const durationText = input.video.durationReadable === false ? '未能读取时长' : `${input.video.durationSeconds} 秒`;
  const eventLines = scenario.keyEvents.map((event, index) => `${index + 1}. ${event}`).join('；');
  const objects = scenario.possibleObjects.slice(0, 6).join('、');
  const hints = scenario.qaHints.slice(0, 2).join('；');

  return [
    `【MOCK MODE】本次演示不会宣称已真实理解上传视频画面；系统仅使用文件名、大小、格式、时长等浏览器元数据，并结合样例场景生成摘要。`,
    `视频整体内容概括：根据文件名“${input.video.name}”、用户指令和 mock 场景匹配，当前最接近“${scenario.title}”。${scenario.summaryTemplate}`,
    `关键事件顺序：${eventLines}。这些事件用于模拟摘要呈现效果，并非对本地视频逐帧识别后的事实断言。`,
    `与用户问题相关的信息：用户指令为“${input.instruction}”。系统优先围绕该指令组织摘要，参考线索包括：${hints}。可能涉及的画面元素示例为：${objects}。`,
    `系统处理说明：本次演示在 mock mode 下运行，文件格式为 ${input.video.fileType || '未知格式'}，文件时长为 ${durationText}，采样帧数为 ${metrics.sampledFrames}；Video Swin、双轨 Token 压缩和 MLP Adapter 为演示链路指标。`,
    `Token 压缩说明：单帧 raw Patch Tokens = ${metrics.rawPatchTokensPerFrame}，Content Token = ${metrics.contentTokensPerFrame}，Context Token = ${metrics.contextTokensPerFrame}，压缩后为 ${metrics.rawPatchTokensPerFrame} → ${metrics.compressedTokensPerFrame}，压缩倍数约 ${metrics.compressionRatio.toFixed(1)}×。真实语义理解需使用 backend mode + inference-python 解码视频帧。`,
  ];
}

export async function runMockInference(input: CreateTaskInput, onEvent: (event: MockInferenceEvent) => void): Promise<void> {
  const scenario = selectScenario(input.video.name, input.instruction, input.video.source === 'sample');
  const stageEvents: MockStageEvent[] = [
    {
      type: 'stage',
      stage: 'waiting',
      status: 'waiting',
      progress: 8,
      detail: '任务已写入 mock 队列；页面将明确展示 mock mode 边界。',
    },
    {
      type: 'stage',
      stage: 'video_sampling',
      status: 'running',
      progress: 18,
      detail: '读取浏览器元数据并按 1 FPS 估算采样帧数；mock mode 不解码本地视频画面。',
    },
    {
      type: 'stage',
      stage: 'video_swin',
      status: 'running',
      progress: 35,
      detail: `匹配 mock 场景：${scenario.title}；单帧原始 Patch Token 指标为 196。`,
    },
    {
      type: 'stage',
      stage: 'content_token',
      status: 'running',
      progress: 50,
      detail: 'Content Token 分支模拟保留主体内容、动作和关键事件线索。',
    },
    {
      type: 'stage',
      stage: 'context_token',
      status: 'running',
      progress: 62,
      detail: 'Context Token 分支模拟保留时间上下文、场景关联和用户指令相关信息。',
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
      detail: 'LLM 生成模块开始以 SSE 风格输出结构化 mock 摘要。',
    },
  ];

  for (const event of stageEvents) {
    await wait(520);
    onEvent(event);
  }

  const summaryLines = buildSummaryLines(input, scenario);
  for (const text of summaryLines) {
    await wait(700);
    onEvent({ type: 'delta', text });
  }

  await wait(460);
  onEvent({
    type: 'done',
    result: {
      summary: summaryLines.join(''),
      keyEvents: scenario.keyEvents,
      conclusion: 'mock mode 已完成演示摘要输出；如需真实视频语义理解，请切换 backend mode 并调用 inference-python 解码视频帧。',
    },
  });
}
