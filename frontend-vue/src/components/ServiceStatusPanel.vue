<template>
  <section class="card service-status-card">
    <div class="card-heading compact-heading">
      <div>
        <h2>服务状态</h2>
        <p>正式分析需要页面、业务服务与分析服务同时在线；本地演示不受影响。</p>
      </div>
      <button class="secondary-button compact-button" type="button" :disabled="checking" @click="checkServices">
        {{ checking ? '检测中' : '重新检测' }}
      </button>
    </div>

    <div class="service-grid">
      <div class="service-item online">
        <span class="status-dot"></span>
        <div>
          <strong>前端</strong>
          <small>已运行</small>
        </div>
      </div>
      <div class="service-item" :class="javaOnline ? 'online' : 'offline'">
        <span class="status-dot"></span>
        <div>
          <strong>Java 后端</strong>
          <small>http://localhost:8080/api/health · {{ javaOnline ? '在线' : '离线' }}</small>
        </div>
      </div>
      <div class="service-item" :class="pythonOnline ? 'online' : 'offline'">
        <span class="status-dot"></span>
        <div>
          <strong>Python 分析服务</strong>
          <small>http://localhost:8000/health · {{ pythonOnline ? '在线' : '离线' }}</small>
        </div>
      </div>
      <div class="service-item optional">
        <span class="status-dot"></span>
        <div>
          <strong>Redis</strong>
          <small>可选扩展；灯开关正式分析不要求启动</small>
        </div>
      </div>
    </div>

    <p v-if="!formalReady" class="status-hint">{{ hintText }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { checkJavaHealth, checkPythonHealth } from '../api/taskApi';

const emit = defineEmits<{
  'status-change': [status: { javaOnline: boolean; pythonOnline: boolean }];
}>();

const checking = ref(false);
const javaOnline = ref(false);
const pythonOnline = ref(false);

const formalReady = computed(() => javaOnline.value && pythonOnline.value);
const hintText = computed(() => {
  if (!javaOnline.value) return 'Java 后端未连接，请先运行一键启动脚本或启动 8080 服务。';
  if (!pythonOnline.value) return 'Python 分析服务未连接，请先运行一键启动脚本或启动 8000 服务。';
  return '';
});

async function checkServices() {
  checking.value = true;
  const [javaOk, pythonOk] = await Promise.all([checkJavaHealth(), checkPythonHealth()]);
  javaOnline.value = javaOk;
  pythonOnline.value = pythonOk;
  checking.value = false;
  emit('status-change', { javaOnline: javaOk, pythonOnline: pythonOk });
}

onMounted(() => {
  void checkServices();
});
</script>
