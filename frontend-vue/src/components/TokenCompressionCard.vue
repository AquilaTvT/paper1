<template>
  <section class="card token-card">
    <div class="card-heading">
      <div>
        <h2>双轨 Token 压缩指标</h2>
        <p>单帧视觉 Patch Token 从 196 压缩到 5，用于展示摘要生成前的视觉信息压缩效果。</p>
      </div>
      <strong>{{ metrics.compressionText }}</strong>
    </div>

    <div class="token-hero">
      <div>
        <span>196 Patch Tokens / frame</span>
        <strong>{{ metrics.rawPatchTokensPerFrame }}</strong>
      </div>
      <div class="arrow">↓</div>
      <div>
        <span>Content Token {{ metrics.contentTokensPerFrame }}</span>
        <strong>{{ metrics.contentTokensPerFrame }} + {{ metrics.contextTokensPerFrame }}</strong>
        <span>Context Token {{ metrics.contextTokensPerFrame }}</span>
      </div>
      <div class="arrow">↓</div>
      <div>
        <span>5 Visual Tokens / frame</span>
        <strong>{{ metrics.compressedTokensPerFrame }}</strong>
      </div>
    </div>

    <div class="metrics-grid compact">
      <div>
        <span>Content Token</span>
        <strong>{{ metrics.contentTokensPerFrame }}</strong>
      </div>
      <div>
        <span>Context Token</span>
        <strong>{{ metrics.contextTokensPerFrame }}</strong>
      </div>
      <div>
        <span>采样率</span>
        <strong>{{ metrics.frameSampleRate }} FPS</strong>
      </div>
      <div>
        <span>压缩倍数</span>
        <strong>{{ metrics.compressionRatio.toFixed(1) }}×</strong>
      </div>
    </div>

    <div class="token-estimate">
      <p>按视频时长 {{ metrics.durationSeconds }} 秒估算，采样帧数 {{ metrics.sampledFrames }} 帧。</p>
      <div class="estimate-bars">
        <label>
          <span>原始总视觉 Token：{{ metrics.rawTotalTokens }}</span>
          <i class="raw" :style="{ width: '100%' }"></i>
        </label>
        <label>
          <span>压缩后视觉 Token：{{ metrics.compressedTotalTokens }}</span>
          <i class="compressed" :style="{ width: `${Math.max(4, 100 / metrics.compressionRatio)}%` }"></i>
        </label>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { TokenMetrics } from '../types/metrics';

defineProps<{
  metrics: TokenMetrics;
}>();
</script>
