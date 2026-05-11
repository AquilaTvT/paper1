<template>
  <main class="dashboard-page">
    <AppHeader />
    <ErrorBanner :message="errorMessage || streamError || serviceMessage" />

    <section class="card mode-selector-card">
      <div class="card-heading">
        <div>
          <h2>分析入口</h2>
          <p>本地演示可直接体验页面流程；正式分析会提交视频并等待真实分析结果。</p>
        </div>
      </div>
      <div class="mode-toggle" role="group" aria-label="分析入口">
        <button type="button" :class="{ active: mode === 'local' }" @click="setMode('local')">本地演示</button>
        <button type="button" :class="{ active: mode === 'formal' }" @click="setMode('formal')">正式分析</button>
      </div>
    </section>

    <ServiceStatusPanel
      :mode="mode"
      :java-connected="serviceStatus.javaConnected"
      :python-connected="serviceStatus.pythonConnected"
      :message="serviceStatus.message"
      @refresh="checkServiceStatus"
    />

    <div class="layout-grid top-grid">
      <VideoUploadPanel :video="selectedVideo" :mode="mode" @file-selected="handleFileSelected" @use-sample="useSampleVideo" />
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
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { createVideoFromFile } from '../api/mockApi';
import { getServiceHealth } from '../api/healthApi';
import { uploadVideo } from '../api/taskApi';
import AppHeader from '../components/AppHeader.vue';
import ArchitectureFlow from '../components/ArchitectureFlow.vue';
import ErrorBanner from '../components/ErrorBanner.vue';
import HistoryTable from '../components/HistoryTable.vue';
import MetricsPanel from '../components/MetricsPanel.vue';
import PromptEditor from '../components/PromptEditor.vue';
import ServiceStatusPanel from '../components/ServiceStatusPanel.vue';
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

type AnalysisMode = 'local' | 'formal';

const selectedVideo = ref<VideoFileInfo | null>(sampleVideo);
const selectedFile = ref<File | null>(null);
const instruction = ref('请判断视频中是否有人按下灯的开关，以及按下后亮度是否变化。');
const mode = ref<AnalysisMode>('local');
const serviceMessage = ref('');
const serviceStatus = reactive({ javaConnected: false, pythonConnected: false, message: '尚未检查服务状态。' });

const { history, finishedCount, addTask, clearHistory } = useLocalHistory();
const mockTask = useMockInferenceTask(addTask);
const backendTask = useBackendInferenceTask(addTask);

const activeRunner = computed(() => (mode.value === 'formal' ? backendTask : mockTask));
const currentTask = computed(() => activeRunner.value.currentTask.value);
const errorMessage = computed(() => activeRunner.value.errorMessage.value);
const isStreaming = computed(() => activeRunner.value.isStreaming.value);
const streamError = computed(() => activeRunner.value.streamError.value);
const runnerCanCreateTask = computed(() => activeRunner.value.canCreateTask.value);

const activeMetrics = computed(() => currentTask.value?.tokenMetrics ?? createTokenMetrics(selectedVideo.value?.durationSeconds ?? sampleVideo.durationSeconds));
const canSubmit = computed(() => {
  const baseReady = Boolean(selectedVideo.value) && instruction.value.trim().length > 0 && runnerCanCreateTask.value;
  if (mode.value === 'local') return baseReady;
  return baseReady && serviceStatus.javaConnected && serviceStatus.pythonConnected && selectedVideo.value?.source === 'upload';
});

function revokeObjectUrl(video: VideoFileInfo | null) {
  if (video?.objectUrl) URL.revokeObjectURL(video.objectUrl);
}

async function checkServiceStatus() {
  serviceMessage.value = '';
  try {
    const response = await getServiceHealth();
    serviceStatus.javaConnected = response.data.status === 'up';
    serviceStatus.pythonConnected = response.data.pythonConnected === true;
    serviceStatus.message = serviceStatus.pythonConnected ? '正式分析服务已就绪。' : '分析服务未连接，请先启动后再使用正式分析。';
  } catch (error) {
    serviceStatus.javaConnected = false;
    serviceStatus.pythonConnected = false;
    serviceStatus.message = '任务服务未连接，请确认服务已启动。';
    serviceMessage.value = error instanceof Error ? error.message : serviceStatus.message;
  }
}

function setMode(nextMode: AnalysisMode) {
  mode.value = nextMode;
  serviceMessage.value = '';
  if (nextMode === 'formal') {
    void checkServiceStatus();
  }
}

async function uploadSelectedFile(localVideo: VideoFileInfo, file: File): Promise<VideoFileInfo> {
  const response = await uploadVideo(file);
  return {
    ...localVideo,
    videoId: response.data.videoId,
    name: response.data.originalFileName ?? response.data.name ?? localVideo.name,
    sizeBytes: response.data.fileSize ?? response.data.sizeBytes ?? localVideo.sizeBytes,
    fileType: localVideo.fileType,
    durationReadable: localVideo.durationReadable,
    source: 'upload',
    objectUrl: localVideo.objectUrl,
    createdAt: response.data.createdAt ?? localVideo.createdAt,
  };
}

async function ensureFormalVideoUploaded(): Promise<boolean> {
  if (!selectedVideo.value || selectedVideo.value.source !== 'upload') {
    serviceMessage.value = '正式分析需要先上传本地视频文件。';
    return false;
  }
  if (!selectedFile.value) return true;

  try {
    selectedVideo.value = await uploadSelectedFile(selectedVideo.value, selectedFile.value);
    selectedFile.value = null;
    return true;
  } catch (error) {
    serviceMessage.value = error instanceof Error ? error.message : '视频上传失败，请检查服务状态。';
    return false;
  }
}

async function handleFileSelected(file: File, metadata: LocalVideoMetadata) {
  const previousVideo = selectedVideo.value;
  const localVideo = createVideoFromFile(file, metadata);
  selectedVideo.value = localVideo;
  selectedFile.value = file;
  revokeObjectUrl(previousVideo);

  if (mode.value !== 'formal') return;
  await ensureFormalVideoUploaded();
}

function useSampleVideo() {
  if (mode.value === 'formal') {
    serviceMessage.value = '正式分析需要先上传本地视频文件。';
    return;
  }
  revokeObjectUrl(selectedVideo.value);
  selectedFile.value = null;
  selectedVideo.value = {
    ...sampleVideo,
    createdAt: new Date().toISOString(),
  };
}

onMounted(checkServiceStatus);
onBeforeUnmount(() => revokeObjectUrl(selectedVideo.value));

async function handleCreateTask() {
  if (!selectedVideo.value) return;
  if (mode.value === 'formal') {
    if (!serviceStatus.javaConnected || !serviceStatus.pythonConnected) {
      serviceMessage.value = '正式分析服务未连接，无法开始分析。';
      return;
    }
    const uploaded = await ensureFormalVideoUploaded();
    if (!uploaded || !selectedVideo.value) return;
  }

  void activeRunner.value.createAndRunTask({
    video: selectedVideo.value,
    instruction: instruction.value,
  });
}
</script>
