import { API_BASE_URL } from './env';
import type { ApiResponse, CreateTaskRequest, TaskListResponse, UploadVideoResponse } from '../types/api';
import type { InferenceTask } from '../types/task';

const API_BASE = API_BASE_URL.replace(/\/$/, '');

async function request<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    ...init,
  });
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || !body.success) {
    throw new Error(body.message || `接口请求失败：${response.status}`);
  }
  return body;
}

export async function uploadVideo(file: File): Promise<ApiResponse<UploadVideoResponse>> {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${API_BASE}/videos/upload`, { method: 'POST', body: formData });
  const body = (await response.json()) as ApiResponse<UploadVideoResponse>;
  if (!response.ok || !body.success) {
    throw new Error(body.message || `视频上传失败：${response.status}`);
  }
  return body;
}

export function createTask(payload: CreateTaskRequest): Promise<ApiResponse<InferenceTask>> {
  return request<InferenceTask>('/tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getTasks(): Promise<ApiResponse<TaskListResponse | InferenceTask[]>> {
  return request<TaskListResponse | InferenceTask[]>('/tasks');
}

export function getTask(taskId: string): Promise<ApiResponse<InferenceTask>> {
  return request<InferenceTask>(`/tasks/${taskId}`);
}
