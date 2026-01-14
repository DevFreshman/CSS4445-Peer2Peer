// File: src/test/java/com/ht/p2p/core/transport/NettyPingPongIT.java
package com.ht.p2p.core.transport;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
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
import com.ht.p2p.core.transport.impl.netty.NettyServer;
import com.ht.p2p.core.transport.impl.netty.NettyTransport;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NettyPingPongIT {

    @Test
    void pingPongOverTcp_keepsCorrelationId() throws Exception {
        Clock clock = Clock.systemUTC();

        NodeContext serverCtx = new NodeContext(
            "it-server",
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );

        NodeContext clientCtx = new NodeContext(
            "it-client",
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );

        RouteRegistry reg = new RouteRegistry();
        PingService ping = new PingServiceImpl();
        reg.register(MessageType.PING, (rctx, inbound) -> RouteResult.ok(ping.handlePing(rctx, inbound)));
        MessageRouter router = new MessageRouter(serverCtx, reg, List.of());

        NettyTransport transport = new NettyTransport();

        try (NettyServer server = transport.startServer(serverCtx, 0, router);
             Connection conn = transport.connect(clientCtx, "127.0.0.1", server.localPort())) {

            CorrelationId corr = CorrelationId.random();
            Envelope req = new Envelope(new Header(MessageType.PING, corr, clock.instant()), "PING");

            Envelope resp = conn.send(req).get(2, TimeUnit.SECONDS);

            assertEquals(MessageType.PONG, resp.header().type());
            assertEquals(corr, resp.header().correlationId());
            assertEquals("PONG", resp.payload());
        } finally {
            serverCtx.eventLoop().shutdownGracefully();
            clientCtx.eventLoop().shutdownGracefully();
        }
    }
}
