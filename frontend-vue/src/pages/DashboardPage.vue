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
import { useMockInferenceTask } from '../composables/useMockInferenceTask';
import type { VideoFileInfo } from '../types/task';
import { createTokenMetrics } from '../utils/tokenMetrics';

const selectedVideo = ref<VideoFileInfo | null>(sampleVideo);
const instruction = ref('请总结视频中的关键事件，并说明 196 → 5 Token 压缩效果。');
const { history, finishedCount, addTask, clearHistory } = useLocalHistory();
const { currentTask, errorMessage, isStreaming, streamError, canCreateTask, createAndRunTask } = useMockInferenceTask(addTask);

const activeMetrics = computed(() => currentTask.value?.tokenMetrics ?? createTokenMetrics(selectedVideo.value?.durationSeconds ?? sampleVideo.durationSeconds));
const canSubmit = computed(() => Boolean(selectedVideo.value) && instruction.value.trim().length > 0 && canCreateTask.value);

function handleFileSelected(file: File) {
  selectedVideo.value = createVideoFromFile(file);
}

function useSampleVideo() {
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
