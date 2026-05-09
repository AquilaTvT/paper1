package com.mmvs.util;

import java.time.Duration;
import java.time.Instant;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static Instant now() {
        return Instant.now();
    }

    public static long elapsedMillis(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0L;
        }
        return Duration.between(start, end).toMillis();
    }
}
