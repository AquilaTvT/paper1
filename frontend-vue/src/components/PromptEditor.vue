<template>
  <section class="card prompt-editor">
    <div class="section-title">
      <span class="step-index">02</span>
      <div>
        <h2>用户指令输入</h2>
        <p>输入摘要或问答指令，创建视频处理任务。</p>
      </div>
    </div>

    <textarea
      :value="modelValue"
      rows="6"
      placeholder="例如：请总结视频中的关键事件，并说明 Token 压缩效果。"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />

    <div class="prompt-actions">
      <button class="primary-button" type="button" :disabled="disabled" @click="$emit('create-task')">
        生成摘要
      </button>
      <p>任务会按进度阶段更新，并在右侧输出摘要。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
defineEmits<{
  'update:modelValue': [value: string];
  'create-task': [];
}>();

defineProps<{
  modelValue: string;
  disabled: boolean;
}>();
</script>
