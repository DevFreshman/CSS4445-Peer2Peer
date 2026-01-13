// com/ht/p2p/core/gossip/SeenCache.java
package com.ht.p2p.core.gossip;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SeenCache {
  private final Duration ttl;
  private final Map<String, Instant> seen = new ConcurrentHashMap<>();

  public SeenCache(Duration ttl) {
    this.ttl = Objects.requireNonNull(ttl);
  }

  public boolean markIfNew(String msgId) {
    Instant now = Instant.now();
    cleanup(now);
    return seen.putIfAbsent(msgId, now) == null;
  }

  private void cleanup(Instant now) {
    Instant cutoff = now.minus(ttl);
    for (var e : seen.entrySet()) {
      if (e.getValue().isBefore(cutoff)) seen.remove(e.getKey(), e.getValue());
    }
  }
}
