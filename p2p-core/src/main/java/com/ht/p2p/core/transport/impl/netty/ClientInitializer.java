// File: src/main/java/com/ht/p2p/core/transport/impl/netty/ClientInitializer.java
package com.ht.p2p.core.transport.impl.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.peer.SessionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.Objects;

public final class ClientInitializer extends ChannelInitializer<SocketChannel> {
    private static final String RESPONSE_HANDLER_KEY = "clientResponseHandler";

    private final NodeContext ctx;
    private final SessionManager sessionManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClientInitializer(NodeContext ctx, SessionManager sessionManager) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        p.addLast("frameDecoder",
            new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
        p.addLast("envelopeDecoder", new EnvelopeDecoder(ctx, mapper));

        p.addLast("framePrepender", new LengthFieldPrepender(4));
        p.addLast("envelopeEncoder", new EnvelopeEncoder(ctx, mapper));

        p.addLast("sessionLifecycle", new SessionLifecycleHandler(ctx, sessionManager));

        p.addLast(RESPONSE_HANDLER_KEY, new NettyConnectionResponseHandler(ctx));
    }

    static void bindConnection(Channel ch, NettyConnection conn) {
        NettyConnectionResponseHandler h = (NettyConnectionResponseHandler) ch.pipeline().get(RESPONSE_HANDLER_KEY);
        if (h != null) {
            h.bind(conn);
        }
    }
}
