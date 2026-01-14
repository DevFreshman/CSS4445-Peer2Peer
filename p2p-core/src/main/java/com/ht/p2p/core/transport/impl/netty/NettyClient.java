// File: src/main/java/com/ht/p2p/core/transport/impl/netty/NettyClient.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.peer.SessionManager;
import com.ht.p2p.core.protocol.*;
import com.ht.p2p.core.transport.TransportException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class NettyClient {
    private final NodeContext ctx;
    private final EventLoopGroup group;
    private final SessionManager sessionManager = new SessionManager();

    public NettyClient(NodeContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.group = new NioEventLoopGroup(1);
    }

    public NettyConnection connect(String host, int port) {
        Objects.requireNonNull(host, "host");

        try {
            ClientInitializer init = new ClientInitializer(ctx, sessionManager);

            Bootstrap b = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(init)
                .option(ChannelOption.TCP_NODELAY, true);

            Channel ch = b.connect(host, port).syncUninterruptibly().channel();

            ctx.logger().info("transport.client.connect", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "remote", host + ":" + port,
                "channelId", ch.id().asShortText()
            ));

            NettyConnection conn = new NettyConnection(ctx, group, ch);

            // wire connection into pipeline handler
            ClientInitializer.bindConnection(ch, conn);

            // PHASE 4: soft-handshake HELLO(peerId=ctx.nodeId) -> await HELLO_ACK
            CorrelationId corr = CorrelationId.random();
            Envelope hello = new Envelope(
                new Header(MessageType.HELLO, corr, ctx.clock().instant()),
                new HelloPayload(ctx.nodeId())
            );

            ctx.logger().info("hello.send", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "channelId", conn.channelId(),
                "corr", corr.value(),
                "peerId", ctx.nodeId()
            ));

            Envelope ack = conn.send(hello).get(2, TimeUnit.SECONDS);
            if (ack.header().type() != MessageType.HELLO_ACK) {
                throw new TransportException("Expected HELLO_ACK, got " + ack.header().type());
            }

            ctx.logger().info("hello.ack", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "channelId", conn.channelId(),
                "corr", corr.value(),
                "payload", String.valueOf(ack.payload())
            ));

            return conn;
        } catch (Exception e) {
            group.shutdownGracefully().syncUninterruptibly();
            throw new TransportException("Failed to connect to " + host + ":" + port, e);
        }
    }

    public SessionManager sessionManager() {
        return sessionManager;
    }
}
