<template>
  <section class="card prompt-editor">
    <div class="section-title">
      <span class="step-index">02</span>
      <div>
        <h2>用户指令输入</h2>
        <p>输入摘要或问答指令，然后开始分析。</p>
      </div>
    </div>

    <textarea
      :value="modelValue"
      rows="6"
      placeholder="例如：请判断视频中是否有人按下灯的开关，以及按下后亮度是否变化。"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />

    <div class="prompt-actions">
      <button class="primary-button" type="button" :disabled="disabled" @click="$emit('create-task')">
        开始分析
      </button>
      <p>{{ hint }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
defineEmits<{
  'update:modelValue': [value: string];
  'create-task': [];
}>();

withDefaults(defineProps<{
  modelValue: string;
  disabled: boolean;
  hint?: string;
}>(), {
  hint: '任务会按进度更新，并输出分析结果。',
});
</script>
