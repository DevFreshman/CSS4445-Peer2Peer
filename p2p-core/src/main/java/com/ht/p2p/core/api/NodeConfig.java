// com/ht/p2p/core/api/NodeConfig.java
package com.ht.p2p.core.api;

import com.ht.p2p.core.identity.NodeId;

import java.time.Duration;

public record NodeConfig(
    NodeId selfId,
    int listenPort,
    String version,
    Duration rpcDefaultTimeout,
    Duration handshakeTimeout,

    // heartbeat
    Duration heartbeatInterval,
    Duration heartbeatTimeout,
    int heartbeatFailThreshold,

    // gossip
    int gossipFanout,
    Duration gossipSeenTtl,

    // dht
    int dhtK,
    int dhtAlpha
) {
  public static NodeConfig defaults(NodeId selfId, int listenPort) {
    return new NodeConfig(
        selfId,
        listenPort,
        "p2p/0.1",
        Duration.ofSeconds(2),
        Duration.ofSeconds(2),

        Duration.ofSeconds(5),
        Duration.ofSeconds(2),
        3,

        3,
        Duration.ofMinutes(5),

        20,
        3
    );
  }
}
