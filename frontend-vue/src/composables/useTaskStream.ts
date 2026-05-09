import { onBeforeUnmount, ref } from 'vue';

export function useTaskStream() {
  const isStreaming = ref(false);
  const streamError = ref('');
  let stopController: AbortController | null = null;

  function startStream() {
    stopController?.abort();
    stopController = new AbortController();
    isStreaming.value = true;
    streamError.value = '';
    return stopController.signal;
  }

  function stopStream() {
    stopController?.abort();
    stopController = null;
    isStreaming.value = false;
  }

  function markStreamError(message: string) {
    streamError.value = message;
    isStreaming.value = false;
  }

  onBeforeUnmount(() => {
    stopStream();
  });

  return {
    isStreaming,
    streamError,
    startStream,
    stopStream,
    markStreamError,
  };
}
