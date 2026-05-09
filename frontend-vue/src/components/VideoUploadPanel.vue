<template>
  <section class="card upload-panel">
    <div class="section-title">
      <span class="step-index">01</span>
      <div>
        <h2>视频上传模块</h2>
        <p>选择本地视频或使用样例视频，后续将对接 POST /api/videos/upload。</p>
      </div>
    </div>

    <label class="upload-dropzone">
      <input type="file" accept="video/*" @change="handleFileChange" />
      <span class="upload-icon">🎞️</span>
      <strong>选择视频文件</strong>
      <small>支持 MP4、MOV、AVI、MKV，当前阶段使用浏览器 mock 元数据。</small>
    </label>

    <button class="secondary-button" type="button" @click="$emit('use-sample')">使用示例视频</button>

    <div v-if="video" class="video-meta">
      <h3>当前视频</h3>
      <dl>
        <div>
          <dt>文件名</dt>
          <dd>{{ video.name }}</dd>
        </div>
        <div>
          <dt>大小</dt>
          <dd>{{ formatBytes(video.sizeBytes) }}</dd>
        </div>
        <div>
          <dt>估算时长</dt>
          <dd>{{ formatDuration(video.durationSeconds) }}</dd>
        </div>
        <div>
          <dt>来源</dt>
          <dd>{{ video.source === 'sample' ? '示例视频' : '本地上传' }}</dd>
        </div>
      </dl>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { VideoFileInfo } from '../types/task';
import { formatBytes, formatDuration } from '../utils/format';

const emit = defineEmits<{
  'file-selected': [file: File];
  'use-sample': [];
}>();

defineProps<{
  video: VideoFileInfo | null;
}>();

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) {
    emit('file-selected', file);
  }
}
</script>
