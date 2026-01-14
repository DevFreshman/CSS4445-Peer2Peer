// File: src/main/java/com/ht/p2p/core/transport/impl/netty/HelloInboundHandler.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.peer.*;
import com.ht.p2p.core.protocol.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class HelloInboundHandler extends SimpleChannelInboundHandler<Envelope> {
    private final NodeContext ctx;
    private final SessionManager sessionManager;
    private final PeerStore peerStore;

    public HelloInboundHandler(NodeContext ctx, SessionManager sessionManager, PeerStore peerStore) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.peerStore = Objects.requireNonNull(peerStore, "peerStore");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext chx, Envelope inbound) {
        if (inbound.header().type() != MessageType.HELLO) {
            chx.fireChannelRead(inbound);
            return;
        }

        String corr = inbound.header().correlationId().value();
        String channelId = chx.channel().id().asShortText();

        HelloPayload hp = parseHelloPayload(inbound.payload());
        PeerId peerId = PeerId.of(hp.peerId());

        Session session = chx.channel().attr(SessionChannelKeys.SESSION_KEY).get();
        Instant now = ctx.clock().instant();

        if (session != null) {
            session.setPeerId(peerId, now);
            sessionManager.bindPeer(session, peerId);
        }

        PeerAddress addr = toPeerAddress(chx);
        peerStore.upsert(new Peer(peerId, addr, now));

        ctx.logger().info("hello.received", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", channelId,
            "sessionId", session == null ? "null" : session.sessionId().value(),
            "peerId", peerId.value(),
            "corr", corr
        ));

        ctx.logger().info("peer.upsert", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "peerId", peerId.value(),
            "address", addr.toString(),
            "lastSeen", now.toString()
        ));

        Envelope ack = new Envelope(
            new Header(MessageType.HELLO_ACK, inbound.header().correlationId(), now),
            "OK"
        );

        chx.writeAndFlush(ack);

        ctx.logger().info("hello.ack", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", channelId,
            "peerId", peerId.value(),
            "corr", corr
        ));
    }

    private HelloPayload parseHelloPayload(Object payload) {
        if (payload instanceof HelloPayload hp) return hp;
        if (payload instanceof Map<?, ?> m) {
            Object v = m.get("peerId");
            if (v != null) return HelloPayload.of(String.valueOf(v));
        }
        if (payload instanceof String s) {
            // allow simplest format: "peerId=xxx" or just "xxx"
            String trimmed = s.trim();
            if (trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                if (parts.length == 2 && parts[0].trim().equalsIgnoreCase("peerId")) {
                    return HelloPayload.of(parts[1].trim());
                }
            }
            return HelloPayload.of(trimmed);
        }
        throw new IllegalArgumentException("Invalid HELLO payload: " + (payload == null ? "null" : payload.getClass().getName()));
    }

    private PeerAddress toPeerAddress(ChannelHandlerContext chx) {
        if (chx.channel().remoteAddress() instanceof InetSocketAddress isa) {
            String host = isa.getAddress() != null ? isa.getAddress().getHostAddress() : isa.getHostString();
            return PeerAddress.of(host, isa.getPort());
        }
        return PeerAddress.of(String.valueOf(chx.channel().remoteAddress()), 0);
    }
}
