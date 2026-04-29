package com.testtarget.service;

import com.testtarget.model.DeserializeLog;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LogService {

    private static final int MAX_LOGS = 200;
    private final ConcurrentLinkedQueue<DeserializeLog> logs = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void addLog(DeserializeLog log) {
        logs.offer(log);
        while (logs.size() > MAX_LOGS) {
            logs.poll();
        }
        notifyEmitters(log);
    }

    public List<DeserializeLog> getRecentLogs(int count) {
        List<DeserializeLog> list = new ArrayList<>(logs);
        Collections.reverse(list);
        if (count > 0 && list.size() > count) {
            list = list.subList(0, count);
        }
        return list;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    private void notifyEmitters(DeserializeLog log) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("log").data(log));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
