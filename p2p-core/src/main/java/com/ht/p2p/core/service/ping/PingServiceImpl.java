// File: src/main/java/com/hoangtien/p2p/core/service/ping/PingServiceImpl.java
package com.ht.p2p.core.service.ping;

import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.MessageType;
import com.ht.p2p.core.router.routes.RouteContext;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class PingServiceImpl implements PingService {

    @Override
    public Envelope handlePing(RouteContext ctx, Envelope ping) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(ping, "ping");

        Logger log = ctx.node().logger();
        CorrelationId corr = ping.header().correlationId();
        Instant ts = ctx.node().clock().instant();

        log.info("ping.received", Map.of(
            LogKeys.NODE_ID, ctx.node().nodeId(),
            "corr", corr.value()
        ));

        Header h = new Header(MessageType.PONG, corr, ts);
        return new Envelope(h, "PONG");
    }
}
