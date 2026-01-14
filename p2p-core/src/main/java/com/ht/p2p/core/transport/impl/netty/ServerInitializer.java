// File: src/main/java/com/ht/p2p/core/transport/impl/netty/ServerInitializer.java
package com.ht.p2p.core.transport.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.peer.PeerStore;
import com.ht.p2p.core.peer.SessionManager;
import com.ht.p2p.core.router.MessageRouter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.Objects;

public final class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private final NodeContext ctx;
    private final MessageRouter router;
    private final SessionManager sessionManager;
    private final PeerStore peerStore;
    private final ObjectMapper mapper = new ObjectMapper();

    public ServerInitializer(NodeContext ctx, MessageRouter router, SessionManager sessionManager, PeerStore peerStore) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.router = Objects.requireNonNull(router, "router");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.peerStore = Objects.requireNonNull(peerStore, "peerStore");
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        // inbound frame -> envelope
        ch.pipeline().addLast("frameDecoder",
            new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
        ch.pipeline().addLast("envelopeDecoder", new EnvelopeDecoder(ctx, mapper));

        // outbound envelope -> frame (outbound goes tail->head)
        ch.pipeline().addLast("framePrepender", new LengthFieldPrepender(4));
        ch.pipeline().addLast("envelopeEncoder", new EnvelopeEncoder(ctx, mapper));

        // session + hello + router
        ch.pipeline().addLast("sessionLifecycle", new SessionLifecycleHandler(ctx, sessionManager));
        ch.pipeline().addLast("helloInbound", new HelloInboundHandler(ctx, sessionManager, peerStore));
        ch.pipeline().addLast("routerInbound", new RouterInboundHandler(ctx, router));
    }
}
