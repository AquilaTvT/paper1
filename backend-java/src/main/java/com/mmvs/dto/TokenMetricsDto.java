package com.mmvs.dto;

public record TokenMetricsDto(
        int sampledFrames,
        int rawPatchTokensPerFrame,
        int compressedTokensPerFrame,
        int rawVisualTokens,
        int compressedVisualTokens,
        double compressionRatio,
        long estimatedLatencyMs
) {
}
