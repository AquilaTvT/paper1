<template>
  <main class="dashboard-page">
    <AppHeader />
    <ErrorBanner :message="errorMessage || streamError || formalBlockMessage" />

    <ServiceStatusPanel @status-change="handleServiceStatus" />

    <section class="card mode-card">
      <div class="card-heading compact-heading">
        <div>
          <h2>分析方式</h2>
          <p>本地演示可直接体验页面流程；正式分析会提交到本机服务并返回真实分析结果。</p>
        </div>
      </div>
      <div class="mode-switch" role="group" aria-label="分析方式">
        <button type="button" :class="{ active: analysisMode === 'local' }" @click="setAnalysisMode('local')">本地演示</button>
        <button type="button" :class="{ active: analysisMode === 'formal' }" @click="setAnalysisMode('formal')">正式分析</button>
      </div>
      <p class="mode-help">{{ analysisMode === 'formal' ? '正式分析流程：页面上传视频，服务读取采样帧并生成分析结果。' : '本地演示不会提交视频，适合快速查看交互与摘要样式。' }}</p>
    </section>

    <div class="layout-grid top-grid">
      <VideoUploadPanel :video="selectedVideo" :analysis-mode="analysisMode" @file-selected="handleFileSelected" @use-sample="useSampleVideo" />
      <PromptEditor v-model="instruction" :disabled="!canSubmit" :hint="submitHint" @create-task="handleCreateTask" />
    </div>

    <div class="layout-grid main-grid">
      <TaskStatusTimeline :task="currentTask" />
      <StreamingSummary :chunks="currentTask?.streamChunks ?? []" :is-streaming="isStreaming" />
    </div>

    <div class="layout-grid insight-grid">
      <TokenCompressionCard :metrics="activeMetrics" />
      <MetricsPanel :task="currentTask" :metrics="activeMetrics" :finished-count="finishedCount" />
    </div>

    <HistoryTable :tasks="history" @clear="clearHistory" />
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import { createVideoFromFile } from '../api/mockApi';
import { uploadVideo } from '../api/taskApi';
import AppHeader from '../components/AppHeader.vue';
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

const formalPrompt = '请判断视频中是否有人按下灯的开关，以及按下后亮度是否变化。';
const localPrompt = '请总结视频中的关键事件，重点关注人物动作和场景变化。';

const selectedVideo = ref<VideoFileInfo | null>(sampleVideo);
const instruction = ref(import.meta.env.VITE_API_MODE === 'backend' ? formalPrompt : localPrompt);
const analysisMode = ref<AnalysisMode>(import.meta.env.VITE_API_MODE === 'backend' ? 'formal' : 'local');
const serviceStatus = ref({ javaOnline: false, pythonOnline: false });
const { history, finishedCount, addTask, clearHistory } = useLocalHistory();
const localTask = useMockInferenceTask(addTask);
const formalTask = useBackendInferenceTask(addTask);

const activeRunner = computed(() => (analysisMode.value === 'formal' ? formalTask : localTask));
const currentTask = computed(() => activeRunner.value.currentTask.value);
const errorMessage = computed(() => activeRunner.value.errorMessage.value);
const isStreaming = computed(() => activeRunner.value.isStreaming.value);
const streamError = computed(() => activeRunner.value.streamError.value);
const canCreateTask = computed(() => activeRunner.value.canCreateTask.value);

const formalReady = computed(() => serviceStatus.value.javaOnline && serviceStatus.value.pythonOnline);
const formalVideoReady = computed(() => analysisMode.value !== 'formal' || selectedVideo.value?.source === 'upload');
const activeMetrics = computed(() => currentTask.value?.tokenMetrics ?? createTokenMetrics(selectedVideo.value?.durationSeconds ?? sampleVideo.durationSeconds));
const formalBlockMessage = computed(() => {
  if (analysisMode.value !== 'formal') return '';
  if (!serviceStatus.value.javaOnline) return 'Java 后端未连接，请先运行一键启动脚本或启动 8080 服务。';
  if (!serviceStatus.value.pythonOnline) return 'Python 分析服务未连接，请先运行一键启动脚本或启动 8000 服务。';
  if (!formalVideoReady.value) return '正式分析需要上传本地视频文件。';
  return '';
});
const submitHint = computed(() => formalBlockMessage.value || '任务会按进度更新，并输出分析结果。');
const canSubmit = computed(() => Boolean(selectedVideo.value) && instruction.value.trim().length > 0 && canCreateTask.value && (analysisMode.value === 'local' || (formalReady.value && formalVideoReady.value)));

function revokeObjectUrl(video: VideoFileInfo | null) {
  if (video?.objectUrl) URL.revokeObjectURL(video.objectUrl);
}

function handleServiceStatus(status: { javaOnline: boolean; pythonOnline: boolean }) {
  serviceStatus.value = status;
}

function setAnalysisMode(mode: AnalysisMode) {
  analysisMode.value = mode;
  if (mode === 'formal' && instruction.value === localPrompt) instruction.value = formalPrompt;
  if (mode === 'local' && instruction.value === formalPrompt) instruction.value = localPrompt;
}

async function handleFileSelected(file: File, metadata: LocalVideoMetadata) {
  const previousVideo = selectedVideo.value;
  const localVideo = createVideoFromFile(file, metadata);
  selectedVideo.value = localVideo;
  revokeObjectUrl(previousVideo);
  if (analysisMode.value !== 'formal') return;

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
      objectUrl: localVideo.objectUrl,
      createdAt: response.data.createdAt ?? localVideo.createdAt,
    };
  } catch (error) {
    revokeObjectUrl(localVideo);
    selectedVideo.value = null;
    const message = error instanceof Error ? error.message : '视频上传失败，请确认 Java 后端已启动。';
    window.alert(message);
  }
}

function useSampleVideo() {
  if (analysisMode.value === 'formal') {
    window.alert('正式分析需要上传本地视频文件。');
    return;
  }
  revokeObjectUrl(selectedVideo.value);
  selectedVideo.value = {
    ...sampleVideo,
    createdAt: new Date().toISOString(),
  };
}

onBeforeUnmount(() => revokeObjectUrl(selectedVideo.value));

function handleCreateTask() {
  if (!selectedVideo.value || !canSubmit.value) return;
  void activeRunner.value.createAndRunTask({
    video: selectedVideo.value,
    instruction: instruction.value,
  });
}
</script>
