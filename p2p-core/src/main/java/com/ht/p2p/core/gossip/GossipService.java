// com/ht/p2p/core/gossip/GossipService.java
package com.ht.p2p.core.gossip;

import com.ht.p2p.core.api.NodeConfig;
import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.Transport;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class GossipService {
  private final NodeConfig cfg;
  private final Transport transport;
  private final SeenCache seen;

  public GossipService(NodeConfig cfg, Transport transport) {
    this.cfg = Objects.requireNonNull(cfg);
    this.transport = Objects.requireNonNull(transport);
    this.seen = new SeenCache(cfg.gossipSeenTtl());
  }

  public CompletableFuture<Void> publish(String topic, byte[] payload) {
    // TODO: build GossipMsg + envelope then send fanout peers via PeerSelector
    return CompletableFuture.completedFuture(null);
  }

  public void onGossip(Session from, Object gossipMsg /* replace with GossipMsg */) {
    // TODO: seen-cache + forward by fanout
  }
}
