// File: src/main/java/com/hoangtien/p2p/core/router/EnvelopeHandler.java
package com.ht.p2p.core.router;

import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.router.routes.RouteContext;
import com.ht.p2p.core.router.routes.RouteResult;

@FunctionalInterface
public interface EnvelopeHandler {
    RouteResult handle(RouteContext ctx, Envelope inbound);
}
