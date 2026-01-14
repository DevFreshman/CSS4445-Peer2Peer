// File: src/main/java/com/hoangtien/p2p/core/NodeContext.java
package com.ht.p2p.core;

import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.EventLoop;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.util.Errors;

import java.time.Clock;

/**
 * Phase 0 shared context. Keep small and explicit.
 */
public final class NodeContext {
    private final String nodeId;
    private final NodeConfig config;
    private final Logger logger;
    private final Clock clock;
    private final EventLoop eventLoop;

    public NodeContext(String nodeId, NodeConfig config, Logger logger, Clock clock, EventLoop eventLoop) {
        this.nodeId = Errors.requireNonNull(nodeId, "nodeId");
        this.config = Errors.requireNonNull(config, "config");
        this.logger = Errors.requireNonNull(logger, "logger");
        this.clock = Errors.requireNonNull(clock, "clock");
        this.eventLoop = Errors.requireNonNull(eventLoop, "eventLoop");
    }

    public String nodeId() {
        return nodeId;
    }

    public NodeConfig config() {
        return config;
    }

    public Logger logger() {
        return logger;
    }

    public Clock clock() {
        return clock;
    }

    public EventLoop eventLoop() {
        return eventLoop;
    }
}
