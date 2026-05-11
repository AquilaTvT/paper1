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
  const durationText = input.video.durationReadable === false ? '时长未能准确读取' : `约 ${input.video.durationSeconds} 秒`;
  const eventLines = scenario.keyEvents.map((event, index) => `${index + 1}. ${event}`).join('；');
  const objects = scenario.possibleObjects.slice(0, 5).join('、');

  if (scenario.scenarioId === 'light-switch') {
    return [
      '演示模式：以下结果基于样例场景与本地元数据生成。',
      '视频摘要：画面中，一只手臂靠近墙面上的白色灯开关，手指随后完成一次按压动作。',
      '关键变化：按压动作发生在开关位置附近，场景亮度可能在操作前后出现变化，较可能对应一次开灯或关灯操作。',
      `事件顺序：${eventLines}。`,
      `补充信息：文件名为“${input.video.name}”，视频${durationText}，用户关注点为“${input.instruction}”。摘要围绕手臂动作、开关位置和亮度变化组织。`,
      `处理指标：按 ${metrics.frameSampleRate} FPS 估算采样 ${metrics.sampledFrames} 帧，视觉 Token 由 ${metrics.rawPatchTokensPerFrame} 压缩至 ${metrics.compressedTokensPerFrame}。`,
    ];
  }

  return [
    '演示模式：以下结果基于样例场景与本地元数据生成。',
    `视频摘要：${scenario.summaryTemplate}`,
    `关键事件：${eventLines}。`,
    `与用户问题相关的信息：用户指令为“${input.instruction}”，摘要优先围绕该问题整理内容。可参考的画面元素包括：${objects}。`,
    `基础信息：文件名为“${input.video.name}”，格式为 ${input.video.fileType || '未知格式'}，视频${durationText}。`,
    `处理指标：按 ${metrics.frameSampleRate} FPS 估算采样 ${metrics.sampledFrames} 帧，视觉 Token 由 ${metrics.rawPatchTokensPerFrame} 压缩至 ${metrics.compressedTokensPerFrame}。`,
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
      detail: '任务已创建，等待开始读取视频。',
    },
    {
      type: 'stage',
      stage: 'video_sampling',
      status: 'running',
      progress: 18,
      detail: '读取视频文件，并提取文件名、格式、大小和时长。',
    },
    {
      type: 'stage',
      stage: 'video_swin',
      status: 'running',
      progress: 35,
      detail: `采样关键帧，当前摘要场景倾向于“${scenario.title}”。`,
    },
    {
      type: 'stage',
      stage: 'content_token',
      status: 'running',
      progress: 50,
      detail: '整理主体、物体和动作线索。',
    },
    {
      type: 'stage',
      stage: 'context_token',
      status: 'running',
      progress: 62,
      detail: '结合时间顺序、场景关系和用户指令进行归纳。',
    },
    {
      type: 'stage',
      stage: 'mlp_adapter',
      status: 'running',
      progress: 74,
      detail: '压缩视觉表示，保留摘要所需的主要信息。',
    },
    {
      type: 'stage',
      stage: 'llm_generation',
      status: 'streaming',
      progress: 84,
      detail: '开始逐句生成视频摘要。',
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
      conclusion: scenario.scenarioId === 'light-switch' ? '视频重点呈现一次手按墙面灯开关的动作，亮度变化需结合画面复核。' : '摘要已生成，关键事件按时间顺序整理完成。',
    },
  });
}
