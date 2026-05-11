<template>
  <main class="dashboard-page">
    <AppHeader />
    <ErrorBanner :message="errorMessage || streamError" />

    <div class="layout-grid top-grid">
      <VideoUploadPanel :video="selectedVideo" @file-selected="handleFileSelected" @use-sample="useSampleVideo" />
      <PromptEditor v-model="instruction" :disabled="!canSubmit" @create-task="handleCreateTask" />
    </div>

    <div class="layout-grid main-grid">
      <TaskStatusTimeline :task="currentTask" />
      <StreamingSummary :chunks="currentTask?.streamChunks ?? []" :is-streaming="isStreaming" />
    </div>

    <div class="layout-grid insight-grid">
      <TokenCompressionCard :metrics="activeMetrics" />
      <MetricsPanel :task="currentTask" :metrics="activeMetrics" :finished-count="finishedCount" />
    </div>

    <ArchitectureFlow />
    <HistoryTable :tasks="history" @clear="clearHistory" />
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { createVideoFromFile } from '../api/mockApi';
import { uploadVideo } from '../api/taskApi';
import AppHeader from '../components/AppHeader.vue';
import ArchitectureFlow from '../components/ArchitectureFlow.vue';
import ErrorBanner from '../components/ErrorBanner.vue';
import HistoryTable from '../components/HistoryTable.vue';
import MetricsPanel from '../components/MetricsPanel.vue';
import PromptEditor from '../components/PromptEditor.vue';
import StreamingSummary from '../components/StreamingSummary.vue';
import TaskStatusTimeline from '../components/TaskStatusTimeline.vue';
import TokenCompressionCard from '../components/TokenCompressionCard.vue';
import VideoUploadPanel from '../components/VideoUploadPanel.vue';
import { sampleVideo } from '../data/sampleTasks';
import { useLocalHistory } from '../composables/useLocalHistory';
import { useBackendInferenceTask } from '../composables/useBackendInferenceTask';
import { useMockInferenceTask } from '../composables/useMockInferenceTask';
import type { LocalVideoMetadata, VideoFileInfo } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';

const selectedVideo = ref<VideoFileInfo | null>(sampleVideo);
const instruction = ref('请总结视频中的关键事件，并说明 196 → 5 Token 压缩效果。');
const apiMode = import.meta.env.VITE_API_MODE === 'backend' ? 'backend' : 'mock';
const { history, finishedCount, addTask, clearHistory } = useLocalHistory();
const mockTask = useMockInferenceTask(addTask);
const backendTask = useBackendInferenceTask(addTask);
const taskRunner = apiMode === 'backend' ? backendTask : mockTask;
const { currentTask, errorMessage, isStreaming, streamError, canCreateTask, createAndRunTask } = taskRunner;

const activeMetrics = computed(() => currentTask.value?.tokenMetrics ?? createTokenMetrics(selectedVideo.value?.durationSeconds ?? sampleVideo.durationSeconds));
const canSubmit = computed(() => Boolean(selectedVideo.value) && instruction.value.trim().length > 0 && canCreateTask.value);

async function handleFileSelected(file: File, metadata: LocalVideoMetadata) {
  const localVideo = createVideoFromFile(file, metadata);
  selectedVideo.value = localVideo;
  if (apiMode !== 'backend') return;

  try {
    const response = await uploadVideo(file);
    selectedVideo.value = {
      ...localVideo,
      videoId: response.data.videoId,
      name: response.data.originalFileName ?? response.data.name ?? localVideo.name,
      sizeBytes: response.data.fileSize ?? response.data.sizeBytes ?? localVideo.sizeBytes,
      fileType: localVideo.fileType,
      durationReadable: localVideo.durationReadable,
      source: 'upload',
      createdAt: response.data.createdAt ?? localVideo.createdAt,
    };
  } catch (error) {
    selectedVideo.value = null;
    const message = error instanceof Error ? error.message : '视频上传到 Java 后端失败。';
    window.alert(message);
  }
}

function useSampleVideo() {
  if (apiMode === 'backend') {
    window.alert('backend mode 需要先上传视频文件，以便 Java 后端生成 videoId。');
    return;
  }
  selectedVideo.value = {
    ...sampleVideo,
    createdAt: new Date().toISOString(),
  };
}

function handleCreateTask() {
  if (!selectedVideo.value) return;
  void createAndRunTask({
    video: selectedVideo.value,
    instruction: instruction.value,
  });
}
</script>
