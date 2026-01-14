// File: src/main/java/com/ht/p2p/core/config/NodeConfig.java
package com.ht.p2p.core.config;

import java.util.Objects;

public final class NodeConfig {
    private final NetworkConfig network;
    private final Timeouts timeouts;

    public NodeConfig(NetworkConfig network, Timeouts timeouts) {
        this.network = Objects.requireNonNull(network, "network");
        this.timeouts = Objects.requireNonNull(timeouts, "timeouts");
    }

    public NetworkConfig network() {
        return network;
    }

    public Timeouts timeouts() {
        return timeouts;
    }

    public static NodeConfig defaults() {
        return new NodeConfig(NetworkConfig.defaults(), Timeouts.defaults());
    }
}
