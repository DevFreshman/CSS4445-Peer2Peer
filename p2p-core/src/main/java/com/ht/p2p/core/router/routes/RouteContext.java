// File: src/main/java/com/hoangtien/p2p/core/router/routes/RouteContext.java
package com.ht.p2p.core.router.routes;

import com.ht.p2p.core.NodeContext;

import java.util.Objects;

public final class RouteContext {
    private final NodeContext nodeContext;

    public RouteContext(NodeContext nodeContext) {
        this.nodeContext = Objects.requireNonNull(nodeContext, "nodeContext");
    }

    public NodeContext node() {
        return nodeContext;
    }
}
