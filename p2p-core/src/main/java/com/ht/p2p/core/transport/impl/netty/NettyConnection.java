// File: src/main/java/com/ht/p2p/core/transport/impl/netty/NettyConnection.java
package com.ht.p2p.core.transport.impl.netty;

import com.ht.p2p.core.NodeContext;
import com.ht.p2p.core.observability.LogKeys;
import com.ht.p2p.core.peer.Session;
import com.ht.p2p.core.protocol.CorrelationId;
import com.ht.p2p.core.protocol.Envelope;
import com.ht.p2p.core.transport.Connection;
import com.ht.p2p.core.transport.TransportException;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class NettyConnection implements Connection {
    private static final long DEFAULT_TIMEOUT_MS = 2_000;

    private final NodeContext ctx;
    private final EventLoopGroup group;
    private final Channel channel;

    private final ConcurrentHashMap<String, CompletableFuture<Envelope>> pending = new ConcurrentHashMap<>();

    NettyConnection(NodeContext ctx, EventLoopGroup group, Channel channel) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.group = Objects.requireNonNull(group, "group");
        this.channel = Objects.requireNonNull(channel, "channel");

        // cleanup pending on channel close => no leaks
        this.channel.closeFuture().addListener(f -> failAllPending(new TransportException("Channel closed")));
    }

    public void onInboundResponse(Envelope inbound) {
        String key = inbound.header().correlationId().value();
        CompletableFuture<Envelope> fut = pending.remove(key);
        if (fut != null) {
            fut.complete(inbound);
        } else {
            ctx.logger().warn("transport.client.unmatched_response", Map.of(
                LogKeys.NODE_ID, ctx.nodeId(),
                "channelId", channel.id().asShortText(),
                "corr", key,
                "type", String.valueOf(inbound.header().type())
            ));
        }
    }

    @Override
    public CompletableFuture<Envelope> send(Envelope request) {
        Objects.requireNonNull(request, "request");
        if (!channel.isActive()) {
            CompletableFuture<Envelope> f = new CompletableFuture<>();
            f.completeExceptionally(new TransportException("Channel not active"));
            return f;
        }

        CorrelationId corr = request.header().correlationId();
        String key = corr.value();

        CompletableFuture<Envelope> future = new CompletableFuture<>();
        CompletableFuture<Envelope> existing = pending.putIfAbsent(key, future);
        if (existing != null) {
            future.completeExceptionally(new TransportException("Duplicate correlationId in-flight: " + key));
            return future;
        }

        ScheduledFuture<?> timeout = channel.eventLoop().schedule(() -> {
            CompletableFuture<Envelope> f = pending.remove(key);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(new TransportException("Request timed out after " + DEFAULT_TIMEOUT_MS + "ms corr=" + key));
            }
        }, DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        future.whenComplete((ok, err) -> timeout.cancel(false));

        channel.writeAndFlush(request).addListener(cf -> {
            if (!cf.isSuccess()) {
                CompletableFuture<Envelope> f = pending.remove(key);
                if (f != null && !f.isDone()) {
                    f.completeExceptionally(new TransportException("Write failed", cf.cause()));
                }
            }
        });

        return future;
    }

    @Override
    public Session getSession() {
        return channel.attr(SessionChannelKeys.SESSION_KEY).get();
    }

    @Override
    public String channelId() {
        return channel.id().asShortText();
    }

    @Override
    public String remoteAddress() {
        return String.valueOf(channel.remoteAddress());
    }

    @Override
    public void close() {
        try {
            channel.close().syncUninterruptibly();
        } finally {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }

    void failAllPending(Throwable t) {
        pending.forEach((k, f) -> f.completeExceptionally(t));
        pending.clear();
    }
}
