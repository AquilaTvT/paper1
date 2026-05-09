package com.mmvs.service;

import com.mmvs.dto.StreamEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String taskId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(taskId, emitter));
        emitter.onTimeout(() -> remove(taskId, emitter));
        emitter.onError(throwable -> remove(taskId, emitter));
        return emitter;
    }

    public void send(String taskId, StreamEvent event) {
        List<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters == null || taskEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : taskEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.eventType())
                        .data(event));
            } catch (IOException | IllegalStateException exception) {
                remove(taskId, emitter);
                try {
                    emitter.completeWithError(exception);
                } catch (IllegalStateException ignored) {
                    // 前端主动断开连接时，忽略重复完成导致的状态异常。
                }
            }
        }
    }

    public void complete(String taskId) {
        List<SseEmitter> taskEmitters = emitters.remove(taskId);
        if (taskEmitters == null) {
            return;
        }
        for (SseEmitter emitter : taskEmitters) {
            try {
                emitter.complete();
            } catch (IllegalStateException ignored) {
                // 连接可能已由浏览器关闭。
            }
        }
    }

    private void remove(String taskId, SseEmitter emitter) {
        List<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters != null) {
            taskEmitters.remove(emitter);
            if (taskEmitters.isEmpty()) {
                emitters.remove(taskId);
            }
        }
    }
}
