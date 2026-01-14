// File: src/test/java/com/ht/p2p/core/peer/SessionManagerIT.java
package com.ht.p2p.core.peer;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.protocol.MessageType;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.router.RouteRegistry;
import com.ht.p2p.core.router.routes.RouteResult;
import com.ht.p2p.core.service.ping.PingService;
import com.ht.p2p.core.service.ping.PingServiceImpl;
import com.ht.p2p.core.transport.Connection;
import com.ht.p2p.core.transport.impl.netty.NettyClient;
import com.ht.p2p.core.transport.impl.netty.NettyServer;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerIT {

    @Test
    void sessionCreatedOnConnect_peerBoundOnHello_removedOnClose() throws Exception {
        Clock clock = Clock.systemUTC();

        SessionManager serverSm = new SessionManager();
        InMemoryPeerStore serverPeers = new InMemoryPeerStore();

        NodeContext serverCtx = new NodeContext(
            "it-server",
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );

        RouteRegistry reg = new RouteRegistry();
        PingService ping = new PingServiceImpl();
        reg.register(MessageType.PING, (rctx, inbound) -> RouteResult.ok(ping.handlePing(rctx, inbound)));
        MessageRouter router = new MessageRouter(serverCtx, reg, List.of());

        try (NettyServer server = new NettyServer(serverCtx, 0, router, serverSm, serverPeers)) {
            server.start();

            NodeContext clientCtx = new NodeContext(
                "it-client",
                NodeConfig.defaults(),
                Logger.stdout(clock),
                clock,
                new NodeExecutor()
            );

            NettyClient client = new NettyClient(clientCtx);

            try (Connection conn = client.connect("127.0.0.1", server.localPort())) {
                // connect triggers HELLO automatically; server session should exist quickly
                await(() -> serverSm.size() == 1, Duration.ofSeconds(2));

                Session s = serverSm.sessions().iterator().next();
                assertNotNull(s);
                assertEquals(SessionState.ACTIVE, s.state());

                // after HELLO, peerId should be set
                await(() -> s.peerId() != null, Duration.ofSeconds(2));
                assertEquals("it-client", s.peerId().value());

                assertTrue(serverPeers.get(PeerId.of("it-client")).isPresent());
            }

            // after close client connection, server session should be removed
            await(() -> serverSm.size() == 0, Duration.ofSeconds(2));
            assertEquals(0, serverSm.size());
        } finally {
            serverCtx.eventLoop().shutdownGracefully();
        }
    }

    private static void await(Check c, Duration timeout) throws InterruptedException {
        Instant end = Instant.now().plus(timeout);
        while (Instant.now().isBefore(end)) {
            if (c.ok()) return;
            Thread.sleep(20);
        }
        if (!c.ok()) {
            fail("Condition not met within " + timeout);
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean ok();
    }
}
