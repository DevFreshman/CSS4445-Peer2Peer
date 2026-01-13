package com.ht.p2p.core.dht;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordStore {

  private record Entry(byte[] value, Instant expiresAt) {}

  private final Map<String, Entry> store = new ConcurrentHashMap<>();

  public void put(byte[] key, byte[] value, Duration ttl) {
    store.put(hex(key), new Entry(value.clone(), Instant.now().plus(ttl)));
  }

  public byte[] get(byte[] key) {
    Entry e = store.get(hex(key));
    if (e == null) return null;

    if (Instant.now().isAfter(e.expiresAt())) {
      store.remove(hex(key), e);
      return null;
    }
    return e.value().clone();
  }

  private static String hex(byte[] b) {
    StringBuilder sb = new StringBuilder(b.length * 2);
    for (byte x : b) sb.append(String.format("%02x", x));
    return sb.toString();
  }
}
