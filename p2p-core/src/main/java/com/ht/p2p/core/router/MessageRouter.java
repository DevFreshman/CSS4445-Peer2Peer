// File: src/main/java/com/hoangtien/p2p/core/router/MessageRouter.java
package com.ht.p2p.core.router;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.ErrorEnvelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.MessageType;
import com.ht.p2p.core.router.routes.RouteContext;
import com.ht.p2p.core.router.routes.RouteResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MessageRouter {
    private final NodeContext ctx;
    private final RouteRegistry registry;
    private final List<Middleware> middleware;

    public MessageRouter(NodeContext ctx, RouteRegistry registry, List<Middleware> middleware) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.middleware = (middleware == null) ? List.of() : List.copyOf(middleware);
    }

    public Envelope dispatch(Envelope inbound) {
        Objects.requireNonNull(inbound, "inbound");
        Logger log = ctx.logger();

        try {
            EnvelopeHandler base = registry.getOrThrow(inbound.header().type());
            EnvelopeHandler chained = applyMiddleware(base);

            RouteResult result = chained.handle(new RouteContext(ctx), inbound);
            if (result == null || result.outbound() == null) {
                // Defensive: handler must return an outbound envelope
                return errorEnvelope(inbound, "INTERNAL", "Handler returned null");
            }
            return result.outbound();
        } catch (RouterException re) {
            log.error("router.route_error", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "inType", String.valueOf(inbound.header().type()),
                "corr", inbound.header().correlationId().value(),
                LogKeys.ERROR, re.toString()
            ));
            return errorEnvelope(inbound, "ROUTE_ERROR", re.getMessage() == null ? "Route error" : re.getMessage());
        } catch (Throwable t) {
            log.error("router.internal_error", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "inType", String.valueOf(inbound.header().type()),
                "corr", inbound.header().correlationId().value(),
                LogKeys.ERROR, t.toString()
            ));
            return errorEnvelope(inbound, "INTERNAL", "Unhandled error");
        }
    }

    private EnvelopeHandler applyMiddleware(EnvelopeHandler terminal) {
        EnvelopeHandler next = terminal;
        for (int i = middleware.size() - 1; i >= 0; i--) {
            Middleware mw = middleware.get(i);
            EnvelopeHandler capturedNext = next;
            next = (routeCtx, inbound) -> mw.apply(routeCtx, inbound, capturedNext);
        }
        return next;
    }

    private Envelope errorEnvelope(Envelope inbound, String code, String message) {
        CorrelationId corr = inbound.header().correlationId();
        Instant ts = ctx.clock().instant();
        Header h = new Header(MessageType.ERROR, corr, ts);
        return new Envelope(h, new ErrorEnvelope(code, message));
    }
}
