import { computed, ref } from 'vue';
import { sampleFinishedTask } from '../data/sampleTasks';
import type { InferenceTask } from '../types/task';

const STORAGE_KEY = 'paper1.mock.task.history';

function readHistory(): InferenceTask[] {
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) return [sampleFinishedTask];

  try {
    return JSON.parse(raw) as InferenceTask[];
  } catch {
    return [sampleFinishedTask];
  }
}

export function useLocalHistory() {
  const history = ref<InferenceTask[]>(readHistory());

  function persist() {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value));
  }

  function addTask(task: InferenceTask) {
    const duplicatedIndex = history.value.findIndex((item) => item.taskId === task.taskId);
    if (duplicatedIndex >= 0) {
      history.value.splice(duplicatedIndex, 1);
    }

    history.value.unshift(task);
    history.value = history.value.slice(0, 8);
    persist();
  }

  function clearHistory() {
    history.value = [sampleFinishedTask];
    persist();
  }

  const finishedCount = computed(() => history.value.filter((task) => task.status === 'finished').length);

  return {
    history,
    finishedCount,
    addTask,
    clearHistory,
  };
}
