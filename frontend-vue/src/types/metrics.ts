export interface TokenMetrics {
  rawPatchTokensPerFrame: number;
  compressedTokensPerFrame: number;
  contentTokensPerFrame: number;
  contextTokensPerFrame: number;
  frameSampleRate: number;
  durationSeconds: number;
  sampledFrames: number;
  rawTotalTokens: number;
  compressedTotalTokens: number;
  compressionRatio: number;
  compressionText: string;
  estimatedReductionPercent: number;
}

export interface RuntimeMetrics {
  preprocessMs: number;
  featureExtractMs: number;
  tokenCompressMs: number;
  adapterMs: number;
  generationMs: number;
  totalMs: number;
}
