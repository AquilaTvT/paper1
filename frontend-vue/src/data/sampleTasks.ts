import type { InferenceTask, PipelineStage, VideoFileInfo } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';

export const sampleVideo: VideoFileInfo = {
  videoId: 'sample-video-001',
  name: '毕业设计演示样例视频.mp4',
  sizeBytes: 18_600_000,
  fileType: 'video/mp4',
  durationSeconds: 32,
  durationReadable: true,
  source: 'sample',
  createdAt: new Date().toISOString(),
};

export function createInitialStages(): PipelineStage[] {
  return [
    {
      key: 'waiting',
      title: '任务排队',
      description: 'Java 后端创建任务并写入 Redis 队列',
      status: 'pending',
    },
    {
      key: 'video_sampling',
      title: '视频抽帧',
      description: '按 1 FPS 采样视频帧，形成推理输入',
      status: 'pending',
    },
    {
      key: 'video_swin',
      title: 'Video Swin 特征提取',
      description: '提取时空视觉特征，保留场景与动作信息',
      status: 'pending',
    },
    {
      key: 'content_token',
      title: 'Content Token 压缩',
      description: '保留主体内容、动作和关键事件信息',
      status: 'pending',
    },
    {
      key: 'context_token',
      title: 'Context Token 压缩',
      description: '保留时间上下文、场景关联和指令相关信息',
      status: 'pending',
    },
    {
      key: 'mlp_adapter',
      title: 'MLP Adapter 投影',
      description: '将视觉 Token 映射至摘要生成语义空间',
      status: 'pending',
    },
    {
      key: 'llm_generation',
      title: 'LLM 摘要生成',
      description: '以 SSE 风格逐句输出摘要内容',
      status: 'pending',
    },
    {
      key: 'finished',
      title: '结果完成',
      description: '写入摘要、指标和历史记录',
      status: 'pending',
    },
  ];
}

export const sampleFinishedTask: InferenceTask = {
  taskId: 'task-sample-history',
  video: sampleVideo,
  instruction: '请总结视频中的关键事件，并说明 Token 压缩效果。',
  status: 'finished',
  currentStage: 'finished',
  progress: 100,
  stages: createInitialStages().map((stage) => ({ ...stage, status: 'done' })),
  streamChunks: [
    {
      id: 'sample-chunk-1',
      text: '视频整体内容概括：该样例展示毕业设计系统的视频选择、任务状态、流式摘要和 196 → 5 Token 压缩指标。',
      createdAt: new Date().toISOString(),
    },
  ],
  tokenMetrics: createTokenMetrics(sampleVideo.durationSeconds),
  runtimeMetrics: {
    preprocessMs: 120,
    featureExtractMs: 180,
    tokenCompressMs: 64,
    adapterMs: 42,
    generationMs: 1360,
    totalMs: 1766,
  },
  result: {
    summary: '视频整体内容概括：该样例展示毕业设计系统的视频选择、任务状态、流式摘要和 196 → 5 Token 压缩指标。',
    keyEvents: ['完成视频抽帧', '执行 Video Swin 特征提取', '展示 196 → 5 Token 压缩指标'],
    conclusion: '该结果可用于论文第六章界面截图和第七章测试分析。',
  },
  createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
  updatedAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
  finishedAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
};
