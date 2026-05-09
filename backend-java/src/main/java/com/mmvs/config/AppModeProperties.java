package com.mmvs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppModeProperties {
    private String mode = "in-memory";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isRedisMode() {
        return "redis".equalsIgnoreCase(mode);
    }
}
