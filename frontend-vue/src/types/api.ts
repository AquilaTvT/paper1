import type { InferenceTask, TaskStatus, VideoFileInfo } from './task';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  requestId: string;
}

export interface UploadVideoResponse extends VideoFileInfo {}

export interface CreateTaskRequest {
  videoId: string;
  instruction: string;
  runMode: 'mock' | 'real';
  stream: boolean;
}

export interface TaskListResponse {
  items: InferenceTask[];
  total: number;
}

export interface TaskStatusEvent {
  taskId: string;
  status: TaskStatus;
  progress: number;
  stage: string;
}
