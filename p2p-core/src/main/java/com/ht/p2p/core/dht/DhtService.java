package com.ht.p2p.core.dht;

import com.ht.p2p.core.api.NodeConfig;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class DhtService {
  private final NodeConfig cfg;
  private final RoutingTable routing;
  private final RecordStore records;

  public DhtService(NodeConfig cfg) {
    this.cfg = Objects.requireNonNull(cfg);
    this.routing = new RoutingTable(cfg.dhtK());
    this.records = new RecordStore();
  }

  public CompletableFuture<Void> put(byte[] key, byte[] value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);

    // Demo: store local first. (sau này mới iterative STORE)
    records.put(key, value, Duration.ofMinutes(10));
    return CompletableFuture.completedFuture(null);
  }

  public CompletableFuture<byte[]> get(byte[] key) {
    Objects.requireNonNull(key);

    // Demo: local lookup. (sau này mới iterative FIND_VALUE)
    return CompletableFuture.completedFuture(records.get(key));
  }

  public RoutingTable routingTable() {
    return routing;
  }

  public RecordStore recordStore() {
    return records;
  }
}
