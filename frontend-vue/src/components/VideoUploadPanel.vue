<template>
  <section class="card upload-panel">
    <div class="section-title">
      <span class="step-index">01</span>
      <div>
        <h2>视频上传</h2>
        <p>选择本地视频后，可立即在浏览器中预览并读取基础信息。</p>
      </div>
      <span class="subtle-tag">{{ mode === 'formal' ? '正式分析' : '本地演示' }}</span>
    </div>

    <div class="mode-notice">
      <strong>预览与理解</strong>
      <p>{{ mode === 'formal' ? '本地预览由浏览器完成；分析任务会提交到服务端处理。' : '本地预览只说明文件可播放；摘要结果基于样例场景与基础元数据生成。' }}</p>
    </div>

    <label class="upload-dropzone">
      <input type="file" accept="video/*" @change="handleFileChange" />
      <span class="upload-icon">□</span>
      <strong>{{ video ? '重新选择视频' : '选择视频文件' }}</strong>
      <small>支持 MP4、MOV、AVI、MKV；选择后显示文件名、大小、类型与时长。</small>
    </label>

    <button class="secondary-button" type="button" @click="$emit('use-sample')">使用示例视频</button>

    <div v-if="video" class="video-preview-card">
      <div v-if="video.objectUrl" class="video-player-shell">
        <video :src="video.objectUrl" controls preload="metadata"></video>
      </div>
      <div v-else class="sample-preview-placeholder">
        <strong>示例视频预览</strong>
        <p>示例用于快速体验摘要流程；上传本地文件后会显示可播放的视频播放器。</p>
      </div>

      <div class="video-meta">
        <h3>视频信息</h3>
        <dl>
          <div>
            <dt>文件名</dt>
            <dd>{{ video.name }}</dd>
          </div>
          <div>
            <dt>文件大小</dt>
            <dd>{{ formatBytes(video.sizeBytes) }}</dd>
          </div>
          <div>
            <dt>文件类型</dt>
            <dd>{{ video.fileType || '未知格式' }}</dd>
          </div>
          <div>
            <dt>时长</dt>
            <dd>{{ formatDuration(video.durationSeconds, video.durationReadable !== false) }}</dd>
          </div>
          <div>
            <dt>来源</dt>
            <dd>{{ video.source === 'sample' ? '示例视频' : '本地上传' }}</dd>
          </div>
          <div>
            <dt>摘要依据</dt>
            <dd>{{ mode === 'formal' ? '正式分析结果' : '样例场景与元数据' }}</dd>
          </div>
        </dl>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LocalVideoMetadata, VideoFileInfo } from '../types/task';
import { formatBytes, formatDuration } from '../utils/format';

const emit = defineEmits<{
  'file-selected': [file: File, metadata: LocalVideoMetadata];
  'use-sample': [];
}>();

defineProps<{
  video: VideoFileInfo | null;
  mode: 'local' | 'formal';
}>();

function fallbackDuration(file: File) {
  return Math.max(12, Math.min(180, Math.round(file.size / 560_000)));
}

function readVideoDuration(file: File): Promise<LocalVideoMetadata> {
  return new Promise((resolve) => {
    const objectUrl = URL.createObjectURL(file);
    const video = document.createElement('video');

    const settle = (metadata: LocalVideoMetadata) => {
      URL.revokeObjectURL(objectUrl);
      video.removeAttribute('src');
      video.load();
      resolve(metadata);
    };

    video.preload = 'metadata';
    video.onloadedmetadata = () => {
      const duration = Number.isFinite(video.duration) && video.duration > 0 ? Math.round(video.duration) : fallbackDuration(file);
      settle({ durationSeconds: duration, durationReadable: Number.isFinite(video.duration) && video.duration > 0, fileType: file.type || '未知格式' });
    };
    video.onerror = () => settle({ durationSeconds: fallbackDuration(file), durationReadable: false, fileType: file.type || '未知格式' });
    video.src = objectUrl;
  });
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) {
    const metadata = await readVideoDuration(file);
    emit('file-selected', file, metadata);
    input.value = '';
  }
}
</script>
