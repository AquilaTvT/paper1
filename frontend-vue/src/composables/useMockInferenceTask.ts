import { computed, ref } from 'vue';
import { createMockTask, runMockInference } from '../api/mockApi';
import type { CreateTaskInput, InferenceTask, PipelineStage, PipelineStageKey } from '../types/task';
import { useTaskStream } from './useTaskStream';

function updateStages(stages: PipelineStage[], currentStage: PipelineStageKey, detail: string): PipelineStage[] {
  const stageOrder = stages.findIndex((stage) => stage.key === currentStage);

  return stages.map((stage, index) => {
    if (index < stageOrder) {
      return { ...stage, status: 'done' };
    }

    if (stage.key === currentStage) {
      return { ...stage, status: 'active', detail };
    }

    return { ...stage, status: 'pending' };
  });
}

function finishStages(stages: PipelineStage[]): PipelineStage[] {
  return stages.map((stage) => ({ ...stage, status: 'done' }));
}

export function useMockInferenceTask(onFinished: (task: InferenceTask) => void) {
  const currentTask = ref<InferenceTask | null>(null);
  const errorMessage = ref('');
  const { isStreaming, streamError, startStream, stopStream, markStreamError } = useTaskStream();

  const canCreateTask = computed(() => !isStreaming.value && currentTask.value?.status !== 'running' && currentTask.value?.status !== 'streaming');

  async function createAndRunTask(input: CreateTaskInput) {
    if (!input.instruction.trim()) {
      errorMessage.value = '请输入摘要或问答指令。';
      return;
    }

    errorMessage.value = '';
    const signal = startStream();
    const task = createMockTask(input);
    currentTask.value = task;

    try {
      await runMockInference(input, (event) => {
        if (signal.aborted || !currentTask.value) return;

        const updatedAt = new Date().toISOString();

        if (event.type === 'stage') {
          currentTask.value = {
            ...currentTask.value,
            status: event.status,
            currentStage: event.stage,
            progress: event.progress,
            stages: updateStages(currentTask.value.stages, event.stage, event.detail),
            updatedAt,
          };
        }

        if (event.type === 'delta') {
          currentTask.value = {
            ...currentTask.value,
            streamChunks: [
              ...currentTask.value.streamChunks,
              {
                id: `chunk-${crypto.randomUUID()}`,
                text: event.text,
                createdAt: updatedAt,
              },
            ],
            status: 'streaming',
            progress: Math.min(96, currentTask.value.progress + 3),
            updatedAt,
          };
        }

        if (event.type === 'done') {
          const finishedAt = new Date().toISOString();
          currentTask.value = {
            ...currentTask.value,
            status: 'finished',
            currentStage: 'finished',
            progress: 100,
            stages: finishStages(currentTask.value.stages),
            runtimeMetrics: {
              preprocessMs: 180,
              featureExtractMs: 260,
              tokenCompressMs: 76,
              adapterMs: 48,
              generationMs: 1820,
              totalMs: 2384,
            },
            result: event.result,
            updatedAt: finishedAt,
            finishedAt,
          };
          stopStream();
          onFinished(currentTask.value);
        }
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : '本地摘要流程发生未知错误。';
      markStreamError(message);
      errorMessage.value = message;
      if (currentTask.value) {
        currentTask.value = {
          ...currentTask.value,
          status: 'failed',
          errorMessage: message,
          updatedAt: new Date().toISOString(),
        };
      }
    }
  }

  return {
    currentTask,
    errorMessage,
    isStreaming,
    streamError,
    canCreateTask,
    createAndRunTask,
  };
}
