package com.mmvs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mmvs.inference")
public class InferenceProperties {

    private String mode = "in-memory";
    private long mockStageDelayMs = 700L;
    private long mockSummaryDelayMs = 650L;
    private int frameSampleRate = 1;
    private int rawPatchTokensPerFrame = 196;
    private int compressedTokensPerFrame = 5;
    private boolean redisEnabled = false;
    private boolean h2JpaEnabled = false;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getMockStageDelayMs() {
        return mockStageDelayMs;
    }

    public void setMockStageDelayMs(long mockStageDelayMs) {
        this.mockStageDelayMs = mockStageDelayMs;
    }

    public long getMockSummaryDelayMs() {
        return mockSummaryDelayMs;
    }

    public void setMockSummaryDelayMs(long mockSummaryDelayMs) {
        this.mockSummaryDelayMs = mockSummaryDelayMs;
    }

    public int getFrameSampleRate() {
        return frameSampleRate;
    }

    public void setFrameSampleRate(int frameSampleRate) {
        this.frameSampleRate = frameSampleRate;
    }

    public int getRawPatchTokensPerFrame() {
        return rawPatchTokensPerFrame;
    }

    public void setRawPatchTokensPerFrame(int rawPatchTokensPerFrame) {
        this.rawPatchTokensPerFrame = rawPatchTokensPerFrame;
    }

    public int getCompressedTokensPerFrame() {
        return compressedTokensPerFrame;
    }

    public void setCompressedTokensPerFrame(int compressedTokensPerFrame) {
        this.compressedTokensPerFrame = compressedTokensPerFrame;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public boolean isH2JpaEnabled() {
        return h2JpaEnabled;
    }

    public void setH2JpaEnabled(boolean h2JpaEnabled) {
        this.h2JpaEnabled = h2JpaEnabled;
    }
}
