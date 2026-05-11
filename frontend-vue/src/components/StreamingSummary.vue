<template>
  <section class="card streaming-card">
    <div class="card-heading">
      <div>
        <h2>SSE 流式摘要输出</h2>
        <p>摘要优先呈现内容概括与关键事件；mock mode 会明确标注演示边界。</p>
      </div>
      <span class="live-dot" :class="{ active: isStreaming }">{{ isStreaming ? '流式输出中' : '等待输出' }}</span>
    </div>

    <div class="terminal-window">
      <div class="terminal-bar">
        <span></span><span></span><span></span>
        <em>text/event-stream</em>
      </div>
      <div class="terminal-content">
        <p v-if="chunks.length === 0" class="placeholder">创建任务后，这里将逐句显示结构化摘要结果，前半部分为内容摘要，后半部分为系统处理说明。</p>
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
