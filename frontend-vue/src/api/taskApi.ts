import type { ApiResponse, CreateTaskRequest, TaskListResponse, UploadVideoResponse } from '../types/api';
import type { InferenceTask } from '../types/task';

const API_BASE = '/api';

async function request<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    ...init,
  });

  if (!response.ok) {
    throw new Error(`接口请求失败：${response.status}`);
  }

  return response.json() as Promise<ApiResponse<T>>;
}

export async function uploadVideo(file: File): Promise<ApiResponse<UploadVideoResponse>> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/videos/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`视频上传失败：${response.status}`);
  }

  return response.json() as Promise<ApiResponse<UploadVideoResponse>>;
}

export function createTask(payload: CreateTaskRequest): Promise<ApiResponse<InferenceTask>> {
  return request<InferenceTask>('/tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getTasks(): Promise<ApiResponse<TaskListResponse>> {
  return request<TaskListResponse>('/tasks');
}

export function getTask(taskId: string): Promise<ApiResponse<InferenceTask>> {
  return request<InferenceTask>(`/tasks/${taskId}`);
}
