// File: src/test/java/com/ht/p2p/core/RouterPingTest.java
package com.ht.p2p.core;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.ErrorEnvelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.MessageType;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.router.RouteRegistry;
import com.ht.p2p.core.router.RouterException;
import com.ht.p2p.core.router.routes.RouteContext;
import com.ht.p2p.core.router.routes.RouteResult;
import com.ht.p2p.core.service.ping.PingService;
import com.ht.p2p.core.service.ping.PingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouterPingTest {

    private static NodeContext newCtx() {
        Clock clock = Clock.systemUTC();
        return new NodeContext(
            "test-node",
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );
    }

    private static Envelope ping(CorrelationId corr, Clock clock) {
        return new Envelope(new Header(MessageType.PING, corr, clock.instant()), "PING");
    }

    @Test
    void registerRoutePing_andDispatchToPong_withSameCorrelationId() {
        NodeContext ctx = newCtx();
        RouteRegistry reg = new RouteRegistry();
        MessageRouter router = new MessageRouter(ctx, reg, List.of());

        PingService pingService = new PingServiceImpl();
        reg.register(MessageType.PING, (RouteContext rctx, Envelope inbound) ->
            RouteResult.ok(pingService.handlePing(rctx, inbound))
        );

        CorrelationId corr = CorrelationId.random();
        Envelope inbound = ping(corr, ctx.clock());

        Envelope out = router.dispatch(inbound);

        assertEquals(MessageType.PONG, out.header().type());
        assertEquals(inbound.header().correlationId(), out.header().correlationId());
        assertEquals("PONG", out.payload());
        System.out.println("Type class = " + inbound.header().type().getClass().getName());
        System.out.println("Register type class = " + MessageType.PING.getClass().getName());

    }

    @Test
    void duplicateRegisterThrows() {
        RouteRegistry reg = new RouteRegistry();
        reg.register(MessageType.PING, (c, e) -> RouteResult.ok(e));
        assertThrows(RouterException.class, () -> reg.register(MessageType.PING, (c, e) -> RouteResult.ok(e)));
    }

    @Test
    void noRoute_returnsErrorEnvelope_andDoesNotThrow() {
        NodeContext ctx = newCtx();
        RouteRegistry reg = new RouteRegistry();
        MessageRouter router = new MessageRouter(ctx, reg, List.of());

        CorrelationId corr = CorrelationId.random();
        Envelope inbound = ping(corr, ctx.clock());

        Envelope out = assertDoesNotThrow(() -> router.dispatch(inbound));
        assertEquals(MessageType.ERROR, out.header().type());
        assertEquals(inbound.header().correlationId(), out.header().correlationId());

        assertNotNull(out.payload());
        assertTrue(out.payload() instanceof ErrorEnvelope);
        ErrorEnvelope err = (ErrorEnvelope) out.payload();
        assertEquals("ROUTE_ERROR", err.code());
    }
}