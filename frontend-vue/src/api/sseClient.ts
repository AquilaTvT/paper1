import { API_BASE } from './taskApi';

export type SseEventHandler = (eventName: string, payload: unknown) => void;

export interface TaskEventClient {
  close: () => void;
}

export function connectTaskEvents(taskId: string, onEvent: SseEventHandler, onError?: (error: Event) => void): TaskEventClient {
  const eventSource = new EventSource(`${API_BASE}/tasks/${taskId}/events`);
  const eventNames = ['status', 'stage', 'token_metrics', 'summary_delta', 'completed', 'error'];

  eventNames.forEach((eventName) => {
    eventSource.addEventListener(eventName, (event) => {
      const message = event as MessageEvent<string>;
      const payload = message.data ? JSON.parse(message.data) : {};
      onEvent(eventName, payload);
    });
  });

  eventSource.onerror = (error) => {
    onError?.(error);
  };

  return {
    close: () => eventSource.close(),
  };
}
