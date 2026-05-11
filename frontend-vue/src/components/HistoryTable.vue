<template>
  <section class="card history-card">
    <div class="card-heading">
      <div>
        <h2>历史任务列表</h2>
        <p>任务完成后保留在本机浏览器中，便于回看最近结果。</p>
      </div>
      <button class="ghost-button" type="button" @click="$emit('clear')">重置历史</button>
    </div>

    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>任务 ID</th>
            <th>视频</th>
            <th>状态</th>
            <th>指令</th>
            <th>Token 指标</th>
            <th>完成时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.taskId">
            <td>{{ task.taskId.slice(0, 13) }}...</td>
            <td>{{ task.video.name }}</td>
            <td><span class="status-pill" :class="task.status">{{ task.status }}</span></td>
            <td>{{ task.instruction }}</td>
            <td>{{ task.tokenMetrics.compressionText }}</td>
            <td>{{ task.finishedAt ? formatTime(task.finishedAt) : '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { InferenceTask } from '../types/task';
import { formatTime } from '../utils/format';

defineEmits<{
  clear: [];
}>();

defineProps<{
  tasks: InferenceTask[];
}>();
</script>
