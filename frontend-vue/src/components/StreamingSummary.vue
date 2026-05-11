<template>
  <section class="card streaming-card">
    <div class="card-heading">
      <div>
        <h2>流式摘要结果</h2>
        <p>摘要按句输出，优先呈现画面内容、动作变化与关键事件。</p>
      </div>
      <span class="live-dot" :class="{ active: isStreaming }">{{ isStreaming ? '生成中' : '等待结果' }}</span>
    </div>

    <div class="terminal-window">
      <div class="terminal-bar">
        <span></span><span></span><span></span>
        <em>summary stream</em>
      </div>
      <div class="terminal-content">
        <p v-if="chunks.length === 0" class="placeholder">创建任务后，这里将逐句显示视频摘要与关键事件。</p>
        <p v-for="chunk in chunks" :key="chunk.id" class="stream-line">
          <span>{{ formatTime(chunk.createdAt) }}</span>
          {{ chunk.text }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { StreamChunk } from '../types/task';
import { formatTime } from '../utils/format';

defineProps<{
  chunks: StreamChunk[];
  isStreaming: boolean;
}>();
</script>
