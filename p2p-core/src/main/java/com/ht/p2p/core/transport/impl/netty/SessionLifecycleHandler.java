// File: src/main/java/com/ht/p2p/core/transport/impl/netty/SessionLifecycleHandler.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.peer.Session;
import com.ht.p2p.core.peer.SessionId;
import com.ht.p2p.core.peer.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;
import java.util.Objects;

public final class SessionLifecycleHandler extends ChannelInboundHandlerAdapter {
    private final NodeContext ctx;
    private final SessionManager sessionManager;

    public SessionLifecycleHandler(NodeContext ctx, SessionManager sessionManager) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
    }

    @Override
    public void channelActive(ChannelHandlerContext chx) throws Exception {
        String channelId = chx.channel().id().asShortText();
        String remote = String.valueOf(chx.channel().remoteAddress());
        Session session = new Session(SessionId.random(), channelId, remote, ctx.clock().instant());
        session.activate(ctx.clock().instant());

        chx.channel().attr(SessionChannelKeys.SESSION_KEY).set(session);
        sessionManager.register(session);

        ctx.logger().info("session.created", Map.of(
            LogKeys.NODE_ID, ctx.nodeId(),
            "sessionId", session.sessionId().value(),
            "channelId", session.channelId(),
            "remote", session.remoteAddress(),
            "state", String.valueOf(session.state())
        ));

        super.channelActive(chx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext chx) throws Exception {
        Session session = chx.channel().attr(SessionChannelKeys.SESSION_KEY).getAndSet(null);
        if (session != null) {
            session.close(ctx.clock().instant());
            sessionManager.unregister(session);

            ctx.logger().info("session.closed", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "sessionId", session.sessionId().value(),
                "channelId", session.channelId(),
                "remote", session.remoteAddress(),
                "peerId", session.peerId() == null ? "null" : session.peerId().value(),
                "state", String.valueOf(session.state())
            ));
        }
        super.channelInactive(chx);
    }
}
