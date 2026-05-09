<template>
  <section class="card metrics-panel">
    <div class="card-heading">
      <div>
        <h2>测试指标卡片</h2>
        <p>用于论文第七章系统测试和答辩 PPT 展示。</p>
      </div>
    </div>

    <div class="metrics-grid">
      <div>
        <span>任务进度</span>
        <strong>{{ task?.progress ?? 0 }}%</strong>
      </div>
      <div>
        <span>状态</span>
        <strong>{{ task?.status ?? 'idle' }}</strong>
      </div>
      <div>
        <span>Token 降低比例</span>
        <strong>{{ formatPercent(metrics.estimatedReductionPercent) }}</strong>
      </div>
      <div>
        <span>历史完成任务</span>
        <strong>{{ finishedCount }}</strong>
      </div>
      <div>
        <span>预处理耗时</span>
        <strong>{{ task?.runtimeMetrics.preprocessMs ?? 0 }} ms</strong>
      </div>
      <div>
        <span>生成耗时</span>
        <strong>{{ task?.runtimeMetrics.generationMs ?? 0 }} ms</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { TokenMetrics } from '../types/metrics';
import type { InferenceTask } from '../types/task';
import { formatPercent } from '../utils/format';

defineProps<{
  task: InferenceTask | null;
  metrics: TokenMetrics;
  finishedCount: number;
}>();
</script>
