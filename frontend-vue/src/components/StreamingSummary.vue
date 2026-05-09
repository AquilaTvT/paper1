<template>
  <section class="card streaming-card">
    <div class="card-heading">
      <div>
        <h2>SSE 流式摘要输出</h2>
        <p>前端最终将通过 EventSource 订阅 GET /api/tasks/{taskId}/events。</p>
      </div>
      <span class="live-dot" :class="{ active: isStreaming }">{{ isStreaming ? '流式输出中' : '等待输出' }}</span>
    </div>

    <div class="terminal-window">
      <div class="terminal-bar">
        <span></span><span></span><span></span>
        <em>text/event-stream</em>
      </div>
      <div class="terminal-content">
        <p v-if="chunks.length === 0" class="placeholder">创建任务后，这里将逐句显示摘要生成结果。</p>
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
