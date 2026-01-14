// File: src/main/java/com/hoangtien/p2p/core/NodeBuilder.java
package com.ht.p2p.core;

import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.EventLoop;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.Logger;

import java.time.Clock;
import java.util.UUID;

/**
 * Phase 0 node builder: provides safe defaults.
 */
public final class NodeBuilder {
    private String nodeId;
    private NodeConfig config;
    private Logger logger;
    private Clock clock;
    private EventLoop eventLoop;

    public NodeBuilder setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public NodeBuilder setConfig(NodeConfig config) {
        this.config = config;
        return this;
    }

    public NodeBuilder setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public NodeBuilder setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public NodeBuilder setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
        return this;
    }

    public Node build() {
        String id = (nodeId == null || nodeId.isBlank()) ? UUID.randomUUID().toString() : nodeId;
        Clock c = (clock == null) ? Clock.systemUTC() : clock;
        Logger l = (logger == null) ? Logger.stdout(c) : logger;
        NodeConfig cfg = (config == null) ? NodeConfig.defaults() : config;
        EventLoop loop = (eventLoop == null) ? new NodeExecutor() : eventLoop;

        NodeContext ctx = new NodeContext(id, cfg, l, c, loop);
        return new Node(ctx);
    }
}
