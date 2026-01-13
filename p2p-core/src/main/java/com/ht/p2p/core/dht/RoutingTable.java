package com.ht.p2p.core.dht;

import java.util.List;

public final class RoutingTable {
  private final int k;

  public RoutingTable(int k) {
    this.k = k;
  }

  public int k() { return k; }

  // TODO: Kademlia k-buckets
  public List<String> closestPeers(byte[] target) {
    return List.of();
  }
}
