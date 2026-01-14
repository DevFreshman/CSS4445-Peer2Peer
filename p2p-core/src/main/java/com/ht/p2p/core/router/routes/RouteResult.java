// File: src/main/java/com/hoangtien/p2p/core/router/routes/RouteResult.java
package com.ht.p2p.core.router.routes;

import com.ht.p2p.core.protocol.Envelope;

import java.util.Objects;

public final class RouteResult {
    private final Envelope outbound;

    private RouteResult(Envelope outbound) {
        this.outbound = Objects.requireNonNull(outbound, "outbound");
    }

    public static RouteResult ok(Envelope outbound) {
        return new RouteResult(outbound);
    }

    public Envelope outbound() {
        return outbound;
    }
}
