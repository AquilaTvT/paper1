<template>
  <section class="card service-status-card">
    <div class="card-heading">
      <div>
        <h2>服务状态</h2>
        <p>正式分析需要本机分析服务保持连接；未连接时不会生成演示结果。</p>
      </div>
      <button class="ghost-button" type="button" @click="$emit('refresh')">刷新</button>
    </div>

    <div class="service-status-grid">
      <div>
        <span>任务服务</span>
        <strong :class="javaConnected ? 'ok' : 'bad'">{{ javaConnected ? '已连接' : '未连接' }}</strong>
      </div>
      <div>
        <span>分析服务</span>
        <strong :class="pythonConnected ? 'ok' : 'bad'">{{ pythonConnected ? '已连接' : '未连接' }}</strong>
      </div>
      <div>
        <span>当前入口</span>
        <strong>{{ mode === 'formal' ? '正式分析' : '本地演示' }}</strong>
      </div>
    </div>

    <p v-if="message" class="service-message">{{ message }}</p>
  </section>
</template>

<script setup lang="ts">
defineEmits<{
  refresh: [];
}>();

defineProps<{
  mode: 'local' | 'formal';
  javaConnected: boolean;
  pythonConnected: boolean;
  message: string;
}>();
</script>
