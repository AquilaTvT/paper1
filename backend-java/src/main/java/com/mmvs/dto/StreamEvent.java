package com.mmvs.dto;

import java.time.Instant;

public record StreamEvent(
        String taskId,
        String eventType,
        String stage,
        Object payload,
        Instant createdAt
) {

    public static StreamEvent of(String taskId, String eventType, String stage, Object payload) {
        return new StreamEvent(taskId, eventType, stage, payload, Instant.now());
    }
}
