// File: src/main/java/com/ht/p2p/core/transport/impl/netty/NettyConnectionResponseHandler.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.protocol.Envelope;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.Objects;

public final class NettyConnectionResponseHandler extends SimpleChannelInboundHandler<Envelope> {
    private final NodeContext ctx;
    private volatile NettyConnection conn;

    public NettyConnectionResponseHandler(NodeContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    public void bind(NettyConnection conn) {
        this.conn = Objects.requireNonNull(conn, "conn");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext chx, Envelope inbound) {
        NettyConnection c = this.conn;
        if (c != null) {
            c.onInboundResponse(inbound);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext chx) throws Exception {
        NettyConnection c = this.conn;
        if (c != null) c.failAllPending(new com.ht.p2p.core.transport.TransportException("Channel inactive"));
        super.channelInactive(chx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext chx, Throwable cause) {
        ctx.logger().error("transport.error", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "channelId", chx.channel().id().asShortText(),
            LogKeys.ERROR, cause.toString()
        ));
        NettyConnection c = this.conn;
        if (c != null) c.failAllPending(cause);
        chx.close();
    }
}
