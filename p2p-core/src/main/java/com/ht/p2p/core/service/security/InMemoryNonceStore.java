// File: src/main/java/com/ht/p2p/core/service/security/InMemoryNonceStore.java
package com.ht.p2p.core.service.security;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryNonceStore implements NonceStore {
    private final ConcurrentHashMap<String, Long> expiryByNonce = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final int maxSize;
    private final AtomicLong ops = new AtomicLong();

    public InMemoryNonceStore() {
        this(120_000L, 50_000);
    }

    public InMemoryNonceStore(long ttlMs, int maxSize) {
        this.ttlMs = ttlMs;
        this.maxSize = maxSize;
    }

    @Override
    public boolean seenOrMark(String nonceBase64, long nowEpochMs) {
        Objects.requireNonNull(nonceBase64, "nonceBase64");

        // opportunistic cleanup
        long n = ops.incrementAndGet();
        if ((n & 0x3FF) == 0 || expiryByNonce.size() > maxSize) {
            cleanup(nowEpochMs);
        }

        Long existing = expiryByNonce.get(nonceBase64);
        if (existing != null && existing >= nowEpochMs) {
            return true;
        }

        long exp = nowEpochMs + ttlMs;
        Long prev = expiryByNonce.put(nonceBase64, exp);
        return prev != null && prev >= nowEpochMs;
    }

    private void cleanup(long nowEpochMs) {
        Iterator<Map.Entry<String, Long>> it = expiryByNonce.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < nowEpochMs) {
                it.remove();
            }
        }
        // If still too big, trim arbitrarily (best-effort)
        if (expiryByNonce.size() > maxSize) {
            int toRemove = expiryByNonce.size() - maxSize;
            Iterator<String> it2 = expiryByNonce.keySet().iterator();
            while (it2.hasNext() && toRemove-- > 0) {
                it2.next();
                it2.remove();
            }
        }
    }
}
