export type SseEventHandler = (eventName: string, payload: unknown) => void;

export interface TaskEventClient {
  close: () => void;
}

export function connectTaskEvents(taskId: string, onEvent: SseEventHandler, onError?: (error: Event) => void): TaskEventClient {
  const eventSource = new EventSource(`/api/tasks/${taskId}/events`);
  const eventNames = ['status', 'delta', 'metrics', 'result', 'error', 'done'];

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
