import type { InferenceTask, TaskStatus } from './task';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  errorCode?: string;
  requestId: string;
}

export interface UploadVideoResponse {
  videoId: string;
  originalFileName: string;
  storedPath: string;
  fileSize: number;
  contentType?: string;
  createdAt: string;
}

export interface CreateTaskRequest {
  videoId: string;
  queryText?: string;
  instruction?: string;
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
