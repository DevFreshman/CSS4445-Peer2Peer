// com/ht/p2p/core/security/RateLimiter.java
package com.ht.p2p.core.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public final class RateLimiter {
  private final int maxPerWindow;
  private final Duration window;

  private volatile Instant windowStart = Instant.now();
  private final AtomicInteger count = new AtomicInteger(0);

  public RateLimiter(int maxPerWindow, Duration window) {
    this.maxPerWindow = maxPerWindow;
    this.window = window;
  }

  public synchronized boolean allow() {
    Instant now = Instant.now();
    if (now.isAfter(windowStart.plus(window))) {
      windowStart = now;
      count.set(0);
    }
    return count.incrementAndGet() <= maxPerWindow;
  }
}
