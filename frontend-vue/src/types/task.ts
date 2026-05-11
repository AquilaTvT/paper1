import type { RuntimeMetrics, TokenMetrics } from './metrics';

export type TaskStatus = 'idle' | 'waiting' | 'running' | 'streaming' | 'finished' | 'failed' | 'cancelled';

export type PipelineStageKey =
  | 'waiting'
  | 'video_sampling'
  | 'video_swin'
  | 'content_token'
  | 'context_token'
  | 'mlp_adapter'
  | 'llm_generation'
  | 'finished';


export interface LocalVideoMetadata {
  durationSeconds: number;
  durationReadable: boolean;
  fileType: string;
}

export interface VideoFileInfo {
  videoId: string;
  name: string;
  sizeBytes: number;
  fileType?: string;
  durationSeconds: number;
  durationReadable?: boolean;
  source: 'upload' | 'sample';
  objectUrl?: string;
  createdAt: string;
}

export interface PipelineStage {
  key: PipelineStageKey;
  title: string;
  description: string;
  status: 'pending' | 'active' | 'done' | 'error';
  detail?: string;
}

export interface StreamChunk {
  id: string;
  text: string;
  createdAt: string;
}

export interface SummaryResult {
  summary: string;
  keyEvents: string[];
  conclusion: string;
}

export interface InferenceTask {
  taskId: string;
  video: VideoFileInfo;
  instruction: string;
  status: TaskStatus;
  currentStage: PipelineStageKey;
  progress: number;
  stages: PipelineStage[];
  streamChunks: StreamChunk[];
  tokenMetrics: TokenMetrics;
  runtimeMetrics: RuntimeMetrics;
  result?: SummaryResult;
  errorMessage?: string;
  createdAt: string;
  updatedAt: string;
  finishedAt?: string;
}

export interface CreateTaskInput {
  video: VideoFileInfo;
  instruction: string;
}
