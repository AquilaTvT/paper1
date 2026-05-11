<template>
  <section class="card timeline-card">
    <div class="card-heading">
      <div>
        <h2>处理进度</h2>
        <p>按阶段展示从视频读取到摘要生成的进度变化。</p>
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
    idle: '未开始',
    waiting: '等待中',
    running: '处理中',
    streaming: '生成中',
    finished: '已完成',
    failed: '失败',
    cancelled: '已取消',
  };
  return map[status];
});
</script>
