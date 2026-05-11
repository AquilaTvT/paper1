import { API_BASE } from './taskApi';
import type { ApiResponse } from '../types/api';

export interface ServiceHealth {
  status: string;
  pythonConnected?: boolean;
  time?: string;
}

export async function getServiceHealth(): Promise<ApiResponse<ServiceHealth>> {
  const response = await fetch(`${API_BASE}/health`);
  if (!response.ok) {
    throw new Error(`服务状态检查失败：${response.status}`);
  }
  return response.json() as Promise<ApiResponse<ServiceHealth>>;
}
