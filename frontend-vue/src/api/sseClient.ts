import { API_BASE_URL } from './env';

export type SseEventHandler = (eventName: string, payload: unknown) => void;

export interface TaskEventClient {
  close: () => void;
}

export function connectTaskEvents(taskId: string, onEvent: SseEventHandler, onError?: (error: Event) => void): TaskEventClient {
  const base = API_BASE_URL.replace(/\/$/, '');
  const eventSource = new EventSource(`${base}/tasks/${taskId}/events`);
  const eventNames = ['status', 'stage', 'token_metrics', 'summary_delta', 'completed', 'error', 'delta', 'metrics', 'result', 'done'];

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
