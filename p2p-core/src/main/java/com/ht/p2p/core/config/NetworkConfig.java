// File: src/main/java/com/hoangtien/p2p/core/config/NetworkConfig.java
package com.ht.p2p.core.config;

import java.util.Objects;

/**
 * Phase 0: bootstrap-only network settings (no transport implementation yet).
 */
public final class NetworkConfig {
    private final String bindHost;
    private final int bindPort;

    public NetworkConfig(String bindHost, int bindPort) {
        this.bindHost = Objects.requireNonNull(bindHost, "bindHost");
        if (bindPort < 0 || bindPort > 65535) {
            throw new IllegalArgumentException("bindPort out of range: " + bindPort);
        }
        this.bindPort = bindPort;
    }

    public String bindHost() {
        return bindHost;
    }

    public int bindPort() {
        return bindPort;
    }

    public static NetworkConfig defaults() {
        return new NetworkConfig("0.0.0.0", 0); // 0 => ephemeral, placeholder for later phases
    }

    @Override
    public String toString() {
        return "NetworkConfig{bindHost='" + bindHost + "', bindPort=" + bindPort + "}";
    }
}
