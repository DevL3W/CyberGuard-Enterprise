package com.cyberguard.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Evita que un agente (o una clave filtrada) inunde el sistema de eventos. */
@Component
public class AgentRateLimiter {
    private static final long WINDOW_MS = 60_000L;
    private final int max;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public AgentRateLimiter(@Value("${cyberguard.monitoring.max-events-per-minute:120}") int max) {
        this.max = max;
    }

    public boolean allow(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < now - WINDOW_MS) q.pollFirst();
            if (q.size() >= max) return false;
            q.addLast(now);
            return true;
        }
    }
}
