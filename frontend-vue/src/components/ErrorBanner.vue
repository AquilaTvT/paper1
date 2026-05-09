<template>
  <div v-if="message" class="error-banner" role="alert">
    <strong>提示：</strong>
    <span>{{ message }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message: string;
}>();
</script>
