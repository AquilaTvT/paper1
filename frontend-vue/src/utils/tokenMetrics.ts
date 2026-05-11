import type { TokenMetrics } from '../types/metrics';

const RAW_PATCH_TOKENS_PER_FRAME = 196;
const COMPRESSED_TOKENS_PER_FRAME = 5;
const CONTENT_TOKENS_PER_FRAME = 1;
const CONTEXT_TOKENS_PER_FRAME = 4;
const FRAME_SAMPLE_RATE = 1;

export function createTokenMetrics(durationSeconds: number): TokenMetrics {
  const sampledFrames = Math.max(1, Math.ceil(durationSeconds * FRAME_SAMPLE_RATE));
  const rawTotalTokens = sampledFrames * RAW_PATCH_TOKENS_PER_FRAME;
  const compressedTotalTokens = sampledFrames * COMPRESSED_TOKENS_PER_FRAME;
  const compressionRatio = RAW_PATCH_TOKENS_PER_FRAME / COMPRESSED_TOKENS_PER_FRAME;
  const estimatedReductionPercent = (1 - compressedTotalTokens / rawTotalTokens) * 100;

  return {
    rawPatchTokensPerFrame: RAW_PATCH_TOKENS_PER_FRAME,
    compressedTokensPerFrame: COMPRESSED_TOKENS_PER_FRAME,
    contentTokensPerFrame: CONTENT_TOKENS_PER_FRAME,
    contextTokensPerFrame: CONTEXT_TOKENS_PER_FRAME,
    frameSampleRate: FRAME_SAMPLE_RATE,
    durationSeconds,
    sampledFrames,
    rawTotalTokens,
    compressedTotalTokens,
    compressionRatio,
    compressionText: '196 → 5',
    estimatedReductionPercent,
  };
}
