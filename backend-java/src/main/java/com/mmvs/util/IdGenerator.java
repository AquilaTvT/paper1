package com.mmvs.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static String videoId() {
        return "video-" + UUID.randomUUID();
    }

    public static String taskId() {
        return "task-" + UUID.randomUUID();
    }

    public static String resultId() {
        return "result-" + UUID.randomUUID();
    }

    public static String logId() {
        return "log-" + UUID.randomUUID();
    }

    public static String requestId() {
        return "req-" + UUID.randomUUID();
    }
}
