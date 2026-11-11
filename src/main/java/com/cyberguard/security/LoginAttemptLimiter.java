package com.cyberguard.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bloquea 15 minutos tras varios intentos fallidos. La clave combina conexión y sesión, así quien
 * adivina contraseñas desde otra conexión no puede dejar fuera al usuario legítimo.
 */
@Component
public class LoginAttemptLimiter {
    private static final long WINDOW_MS = 15 * 60_000L;
    private final int maxFailures;
    private final Map<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(@Value("${cyberguard.security.max-login-failures:5}") int maxFailures) {
        this.maxFailures = maxFailures;
    }

    public boolean isBlocked(String key) {
        Deque<Long> q = failures.get(key);
        if (q == null) return false;
        synchronized (q) {
            prune(q);
            return q.size() >= maxFailures;
        }
    }

    public void failure(String key) {
        Deque<Long> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(System.currentTimeMillis());
        }
    }

    public void success(String key) { failures.remove(key); }

    private void prune(Deque<Long> q) {
        long limit = System.currentTimeMillis() - WINDOW_MS;
        while (!q.isEmpty() && q.peekFirst() < limit) q.pollFirst();
    }
}
