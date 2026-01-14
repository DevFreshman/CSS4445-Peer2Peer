// File: src/main/java/com/ht/p2p/Phase4Demo.java
package com.ht.p2p;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.config.NodeConfig;
import com.ht.p2p.core.execution.NodeExecutor;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.observability.Logger;
import com.ht.p2p.core.peer.PeerId;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.protocol.Header;
import com.ht.p2p.core.protocol.MessageType;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.router.RouteRegistry;
import com.ht.p2p.core.router.routes.RouteResult;
import com.ht.p2p.core.service.ping.PingService;
import com.ht.p2p.core.service.ping.PingServiceImpl;
import com.ht.p2p.core.transport.Connection;
import com.ht.p2p.core.transport.impl.netty.NettyServer;
import com.ht.p2p.core.transport.impl.netty.NettyTransport;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Phase4Demo {
    public static void main(String[] args) throws Exception {
        Clock clock = Clock.systemUTC();

        NodeContext serverCtx = new NodeContext(
            "server-node",
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );

        NodeContext clientCtx = new NodeContext(
            "client-node",
            NodeConfig.defaults(),
            Logger.stdout(clock),
            clock,
            new NodeExecutor()
        );

        // server router: PING -> PONG
        RouteRegistry reg = new RouteRegistry();
        PingService ping = new PingServiceImpl();
        reg.register(MessageType.PING, (rctx, inbound) -> RouteResult.ok(ping.handlePing(rctx, inbound)));
        MessageRouter router = new MessageRouter(serverCtx, reg, List.of());

        NettyTransport transport = new NettyTransport();

        try (NettyServer server = transport.startServer(serverCtx, 0, router);
             Connection conn = transport.connect(clientCtx, "127.0.0.1", server.localPort())) {

            // HELLO is sent automatically by NettyClient.connect() in this phase.

            // ✅ FIX: ensure client-side session has peerId for logs + session.closed
            var session = conn.getSession();
            Instant now = clock.instant();
            if (session != null && session.peerId() == null) {
                session.setPeerId(PeerId.of(clientCtx.nodeId()), now);
            }

            clientCtx.logger().info("phase4.session.client", Map.of(
                LogKeys.NODE_ID, clientCtx.nodeId(),
                "channelId", conn.channelId(),
                "remote", conn.remoteAddress(),
                "sessionId", session == null ? "null" : session.sessionId().value(),
                "peerId", session == null || session.peerId() == null ? "null" : session.peerId().value()
            ));

            CorrelationId corr = CorrelationId.random();
            Envelope pingEnv = new Envelope(new Header(MessageType.PING, corr, now), "PING");

            clientCtx.logger().info("phase4.ping.send", Map.of(
                LogKeys.NODE_ID, clientCtx.nodeId(),
                "channelId", conn.channelId(),
                "corr", corr.value()
            ));

            Envelope resp = conn.send(pingEnv).get(2, TimeUnit.SECONDS);

            clientCtx.logger().info("phase4.ping.recv", Map.of(
                LogKeys.NODE_ID, clientCtx.nodeId(),
                "channelId", conn.channelId(),
                "type", String.valueOf(resp.header().type()),
                "corr", resp.header().correlationId().value(),
                "payload", String.valueOf(resp.payload())
            ));
        } finally {
            serverCtx.eventLoop().shutdownGracefully();
            clientCtx.eventLoop().shutdownGracefully();
        }
    }
}
