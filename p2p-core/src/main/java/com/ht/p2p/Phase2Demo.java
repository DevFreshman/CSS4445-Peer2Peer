// File: src/main/java/com/ht/p2p/Phase2Demo.java
package com.ht.p2p;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.MessageType;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.router.RouteRegistry;
import com.ht.p2p.core.router.routes.RouteResult;
import com.ht.p2p.core.service.ping.PingService;
import com.ht.p2p.core.service.ping.PingServiceImpl;

import java.time.Clock;
import java.util.List;

public final class Phase2Demo {
    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        Logger logger = Logger.stdout(clock);

        NodeContext ctx = new NodeContext(
            "demo-node",
            NodeConfig.defaults(),
            logger,
            clock,
            new NodeExecutor()
        );

        RouteRegistry registry = new RouteRegistry();
        MessageRouter router = new MessageRouter(ctx, registry, List.of());

        PingService ping = new PingServiceImpl();
        registry.register(MessageType.PING, (rctx, inbound) -> RouteResult.ok(ping.handlePing(rctx, inbound)));

        CorrelationId corr = CorrelationId.random();
        Envelope inbound = new Envelope(new Header(MessageType.PING, corr, clock.instant()), "PING");

        logger.info("phase2.dispatch",
            Logger.fields(LogKeys.NODE_ID, ctx.nodeId(), "inType", inbound.header().type(), "corr", corr.value()));

        Envelope out = router.dispatch(inbound);

        logger.info("phase2.result",
            Logger.fields(LogKeys.NODE_ID, ctx.nodeId(), "outType", out.header().type(),
                "corr", out.header().correlationId().value(), "payload", String.valueOf(out.payload())));

        ctx.eventLoop().shutdownGracefully();
    }
}
