package com.cyberguard.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Limita cuántas altas por invitación puede intentar una misma dirección en una hora (una oficina comparte dirección). */
@Component
public class JoinRateLimiter {
    private static final long WINDOW_MS = 3_600_000L;
    private final int max;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public JoinRateLimiter(@Value("${cyberguard.invitations.max-joins-per-hour:20}") int max) { this.max = max; }

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
