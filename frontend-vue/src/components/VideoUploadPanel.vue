<template>
  <section class="card upload-panel">
    <div class="section-title">
      <span class="step-index">01</span>
      <div>
        <h2>视频上传模块</h2>
        <p v-if="apiMode === 'backend'">当前为 <strong>BACKEND MODE</strong>：文件会上传到 Java 后端，并由后端生成 videoId。</p>
        <p v-else>当前为 <strong>MOCK MODE</strong>：本地视频仅读取浏览器可获得的文件名、大小、格式和时长元数据。</p>
      </div>
    </div>

    <div class="mode-notice">
      <strong>{{ apiMode === 'backend' ? 'Backend mode 边界说明' : 'Mock mode 边界说明' }}</strong>
      <p v-if="apiMode === 'backend'">backend mode 会把文件上传到本地 Java 后端用于演示链路；默认仍使用 mock/in-memory 或 Redis mock 推理结果，除非单独接入真实模型权重。</p>
      <p v-else>此模式不会读取本地文件路径，也不会把文件内容上传到第三方服务；摘要来自样例场景、用户指令和系统指标，不代表真实画面识别。</p>
    </div>

    <label class="upload-dropzone">
      <input type="file" accept="video/*" @change="handleFileChange" />
      <span class="upload-icon">▣</span>
      <strong>选择视频文件</strong>
      <small>支持 MP4、MOV、AVI、MKV；选择后将读取 fileName、fileSize、fileType 与 duration 元数据。</small>
    </label>

    <button class="secondary-button" type="button" @click="$emit('use-sample')">使用示例视频</button>

    <div v-if="video" class="video-meta">
      <h3>当前视频元数据</h3>
      <dl>
        <div>
          <dt>fileName</dt>
          <dd>{{ video.name }}</dd>
        </div>
        <div>
          <dt>fileSize</dt>
          <dd>{{ formatBytes(video.sizeBytes) }}</dd>
        </div>
        <div>
          <dt>fileType</dt>
          <dd>{{ video.fileType || '未知格式' }}</dd>
        </div>
        <div>
          <dt>duration</dt>
          <dd>{{ formatDuration(video.durationSeconds, video.durationReadable !== false) }}</dd>
        </div>
        <div>
          <dt>来源</dt>
          <dd>{{ video.source === 'sample' ? '示例视频' : '本地上传' }}</dd>
        </div>
        <div>
          <dt>语义理解状态</dt>
          <dd>Mock 摘要，不代表真实画面识别</dd>
        </div>
      </dl>
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
  apiMode: 'mock' | 'backend';
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
  }
}
</script>
