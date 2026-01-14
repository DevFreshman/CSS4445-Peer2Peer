// File: src/main/java/com/ht/p2p/core/transport/impl/netty/RouterInboundHandler.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.router.MessageRouter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.Objects;

public final class RouterInboundHandler extends SimpleChannelInboundHandler<Envelope> {
    private final NodeContext ctx;
    private final MessageRouter router;

    public RouterInboundHandler(NodeContext ctx, MessageRouter router) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.router = Objects.requireNonNull(router, "router");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext chx, Envelope inbound) {
        String channelId = chx.channel().id().asShortText();

        ctx.logger().info("netty.inbound.dispatch", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", channelId,
            "type", String.valueOf(inbound.header().type()),
            "corr", inbound.header().correlationId().value(),
            "sessionId", sessionId(chx)
        ));

        Envelope out = router.dispatch(inbound);

        ctx.logger().info("netty.outbound.dispatch", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", channelId,
            "type", String.valueOf(out.header().type()),
            "corr", out.header().correlationId().value(),
            "sessionId", sessionId(chx)
        ));

        chx.writeAndFlush(out);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext chx, Throwable cause) {
        ctx.logger().error("transport.error", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", chx.channel().id().asShortText(),
            LogKeys.ERROR, cause.toString(),
            "sessionId", sessionId(chx)
        ));
        chx.close();
    }

    private static String sessionId(ChannelHandlerContext chx) {
        var s = chx.channel().attr(SessionChannelKeys.SESSION_KEY).get();
        return s == null ? "null" : s.sessionId().value();
    }
}
