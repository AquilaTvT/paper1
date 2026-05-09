<template>
  <section class="card timeline-card">
    <div class="card-heading">
      <div>
        <h2>任务状态时间线</h2>
        <p>模拟 Redis 队列、Python worker 和 SSE 消息回传的阶段变化。</p>
      </div>
      <span class="status-pill" :class="task?.status || 'idle'">{{ statusText }}</span>
    </div>

    <div class="progress-track">
      <span :style="{ width: `${task?.progress ?? 0}%` }"></span>
    </div>

    <ol class="timeline-list">
      <li v-for="stage in stages" :key="stage.key" :class="stage.status">
        <span class="timeline-dot"></span>
        <div>
          <strong>{{ stage.title }}</strong>
          <p>{{ stage.detail || stage.description }}</p>
        </div>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { createInitialStages } from '../data/sampleTasks';
import type { InferenceTask } from '../types/task';

const props = defineProps<{
  task: InferenceTask | null;
}>();

const stages = computed(() => props.task?.stages ?? createInitialStages());
const statusText = computed(() => {
  const status = props.task?.status ?? 'idle';
  const map = {
    idle: '未创建',
    waiting: 'waiting',
    running: 'running',
    streaming: 'streaming',
    finished: 'finished',
    failed: 'failed',
    cancelled: 'cancelled',
  };
  return map[status];
});
</script>
