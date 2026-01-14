// File: src/main/java/com/ht/p2p/core/transport/impl/netty/NettyServer.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.peer.InMemoryPeerStore;
import com.ht.p2p.core.peer.PeerStore;
import com.ht.p2p.core.peer.SessionManager;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.transport.TransportException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Map;
import java.util.Objects;

public final class NettyServer implements AutoCloseable {
    private final NodeContext ctx;
    private final int port;
    private final MessageRouter router;

    private final SessionManager sessionManager;
    private final PeerStore peerStore;

    private final EventLoopGroup boss;
    private final EventLoopGroup worker;

    private volatile Channel serverChannel;

    public NettyServer(NodeContext ctx, int port, MessageRouter router) {
        this(ctx, port, router, new SessionManager(), new InMemoryPeerStore());
    }

    public NettyServer(NodeContext ctx, int port, MessageRouter router, SessionManager sessionManager, PeerStore peerStore) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.port = port;
        this.router = Objects.requireNonNull(router, "router");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.peerStore = Objects.requireNonNull(peerStore, "peerStore");
        this.boss = new NioEventLoopGroup(1);
        this.worker = new NioEventLoopGroup(2);
    }

    public void start() {
        try {
            ServerBootstrap b = new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerInitializer(ctx, router, sessionManager, peerStore))
                .childOption(ChannelOption.TCP_NODELAY, true);

            this.serverChannel = b.bind(port).syncUninterruptibly().channel();

            ctx.logger().info("transport.server.start", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "port", String.valueOf(localPort())
            ));
        } catch (Exception e) {
            throw new TransportException("Failed to start server", e);
        }
    }

    public int localPort() {
        Channel ch = serverChannel;
        if (ch == null) return -1;
        return ((java.net.InetSocketAddress) ch.localAddress()).getPort();
    }

    public SessionManager sessionManager() {
        return sessionManager;
    }

    public PeerStore peerStore() {
        return peerStore;
    }

    @Override
    public void close() {
        try {
            Channel ch = serverChannel;
            if (ch != null) ch.close().syncUninterruptibly();
        } finally {
            boss.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }
    }
}
