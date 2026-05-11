import type { InferenceTask, PipelineStage, VideoFileInfo } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';

export const sampleVideo: VideoFileInfo = {
  videoId: 'sample-video-001',
  name: '手按白色灯开关示例.mp4',
  sizeBytes: 8_400_000,
  fileType: 'video/mp4',
  durationSeconds: 12,
  durationReadable: true,
  source: 'sample',
  createdAt: new Date().toISOString(),
};

export function createInitialStages(): PipelineStage[] {
  return [
    {
      key: 'waiting',
      title: '读取视频文件',
      description: '等待任务开始，准备读取视频基础信息',
      status: 'pending',
    },
    {
      key: 'video_sampling',
      title: '提取基础元数据',
      description: '读取文件名、大小、格式和时长',
      status: 'pending',
    },
    {
      key: 'video_swin',
      title: '采样关键帧',
      description: '按时间顺序选取具有代表性的画面片段',
      status: 'pending',
    },
    {
      key: 'content_token',
      title: '分析动作片段',
      description: '整理画面主体、物体和动作变化',
      status: 'pending',
    },
    {
      key: 'context_token',
      title: '整理上下文',
      description: '结合时间顺序和用户指令归纳关键线索',
      status: 'pending',
    },
    {
      key: 'mlp_adapter',
      title: '压缩视觉表示',
      description: '保留摘要生成所需的主要信息',
      status: 'pending',
    },
    {
      key: 'llm_generation',
      title: '生成摘要',
      description: '逐句输出视频摘要和关键事件',
      status: 'pending',
    },
    {
      key: 'finished',
      title: '结果完成',
      description: '保存摘要结果、处理指标和历史记录',
      status: 'pending',
    },
  ];
}

export const sampleFinishedTask: InferenceTask = {
  taskId: 'task-sample-history',
  video: sampleVideo,
  instruction: '请总结手按灯开关的关键动作。',
  status: 'finished',
  currentStage: 'finished',
  progress: 100,
  stages: createInitialStages().map((stage) => ({ ...stage, status: 'done' })),
  streamChunks: [
    {
      id: 'sample-chunk-1',
      text: '视频摘要：画面中，一只手臂靠近墙面白色灯开关，并完成一次按压动作。',
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
    summary: '视频摘要：画面中，一只手臂靠近墙面白色灯开关，并完成一次按压动作。按压后场景亮度可能发生变化，可谨慎判断为一次开灯或关灯操作。',
    keyEvents: ['手臂靠近墙面开关', '手指按下白色开关', '按压后观察亮度变化'],
    conclusion: '该片段重点呈现一次灯开关按压动作，亮度变化需要结合画面复核。',
  },
  createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
  updatedAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
  finishedAt: new Date(Date.now() - 1000 * 60 * 18).toISOString(),
};
