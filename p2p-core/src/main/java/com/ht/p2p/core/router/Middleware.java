// File: src/main/java/com/hoangtien/p2p/core/router/Middleware.java
package com.ht.p2p.core.router;

import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.router.routes.RouteContext;
import com.ht.p2p.core.router.routes.RouteResult;

@FunctionalInterface
public interface Middleware {
    RouteResult apply(RouteContext ctx, Envelope inbound, EnvelopeHandler next);
}
