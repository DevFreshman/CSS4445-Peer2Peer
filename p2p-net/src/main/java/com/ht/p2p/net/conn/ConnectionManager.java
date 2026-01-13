package com.ht.p2p.net.conn;

import com.google.protobuf.ByteString;
import com.ht.p2p.net.codec.P2PChannelInitializer;
import com.ht.p2p.net.handler.InboundEnvelopeHandler;
import com.ht.p2p.net.peer.Peer;
import com.ht.p2p.net.rpc.PendingRequests;
import com.ht.p2p.net.transport.NettyClient;
import com.ht.p2p.proto.Envelope;
import com.ht.p2p.proto.HelloReq;
import com.ht.p2p.proto.HelloRes;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionManager implements AutoCloseable {
  private final EventLoopGroup sharedGroup;
  private final PendingRequests pending;
  private final ByteString selfId;
  private final HeartbeatService heartbeat;

  // key = host:port, value = ongoing connect future (dedupe)
  private final Map<String, CompletableFuture<Channel>> inflight = new ConcurrentHashMap<>();

  // key = host:port, value = active channel
  private final Map<String, Channel> channels = new ConcurrentHashMap<>();

  // key = host:port, value = client wrapper (owns bootstrap)
  private final Map<String, NettyClient> clients = new ConcurrentHashMap<>();

  public ConnectionManager(PendingRequests pending, ByteString selfId, HeartbeatService heartbeat) {
    this.pending = Objects.requireNonNull(pending);
    this.selfId = Objects.requireNonNull(selfId);
    this.heartbeat = Objects.requireNonNull(heartbeat);
    this.sharedGroup = new NioEventLoopGroup();
  }

  public CompletableFuture<Channel> getOrConnect(Peer peer) {
    String key = peer.address();

    Channel existing = channels.get(key);
    if (existing != null
        && existing.isActive()
        && Boolean.TRUE.equals(existing.attr(ChannelAttrs.HANDSHAKE_OK).get())) {
      return CompletableFuture.completedFuture(existing);
    }

    // dedupe concurrent connect attempts
    return inflight.computeIfAbsent(key, k ->
        connect(peer).whenComplete((ch, err) -> inflight.remove(k))
    );
  }

  public CompletableFuture<Channel> connect(Peer peer) {
    String key = peer.address();

    // cleanup old
    Channel old = channels.remove(key);
    if (old != null) old.close();

    NettyClient oldClient = clients.remove(key);
    if (oldClient != null) oldClient.close();

    CompletableFuture<Channel> fut = new CompletableFuture<>();

    // ✅ IMPORTANT: P2PChannelInitializer needs Supplier<InboundEnvelopeHandler>
    P2PChannelInitializer initializer = new P2PChannelInitializer(
        () -> new InboundEnvelopeHandler(false, pending, selfId)
    );

    NettyClient client = new NettyClient(sharedGroup, initializer);
    clients.put(key, client);

    try {
      client.connect(peer.host(), peer.port());
      Channel ch = client.channel();

      ch.closeFuture().addListener(ignored -> {
        channels.remove(key);
        NettyClient c = clients.remove(key);
        if (c != null) c.close();
      });

      channels.put(key, ch);

      // start heartbeat AFTER connect
      heartbeat.start(peer, ch);

      // ======== HELLO HANDSHAKE (NO RpcClient here) ========
      helloOnChannel(ch, peer.port(), Duration.ofSeconds(2))
          .thenAccept(res -> {
            ch.attr(ChannelAttrs.REMOTE_NODE_ID).set(res.getNodeId());
            ch.attr(ChannelAttrs.HANDSHAKE_OK).set(true);

            System.out.println("[HELLO] ok peer=" + key
                + " remoteNodeIdBytes=" + res.getNodeId().size());

            fut.complete(ch);
          })
          .exceptionally(err -> {
            System.err.println("[HELLO] failed peer=" + key + " err=" + err);
            ch.close();
            fut.completeExceptionally(err);
            return null;
          });
      // =====================================================

    } catch (Throwable t) {
      clients.remove(key);
      try { client.close(); } catch (Throwable ignore) {}
      fut.completeExceptionally(t);
    }

    return fut;
  }

  // mini-RPC: send HELLO directly on channel (NO getOrConnect)
  private CompletableFuture<HelloRes> helloOnChannel(Channel ch, int listenPort, Duration timeout) {
    String requestId = UUID.randomUUID().toString();
    CompletableFuture<Envelope> raw = pending.register(requestId, timeout);

    HelloReq req = HelloReq.newBuilder()
        .setNodeId(selfId)
        .setListenPort(listenPort)
        .setVersion("p2p/0.1")
        .build();

    Envelope env = Envelope.newBuilder()
        .setSenderId(selfId)
        .setRequestId(requestId)
        .setTimestampMs(Instant.now().toEpochMilli())
        .setHelloReq(req)
        .build();

    ch.writeAndFlush(env).addListener(f -> {
      if (!f.isSuccess()) raw.completeExceptionally(f.cause());
    });

    return raw.thenApply(Envelope::getHelloRes);
  }

  public void disconnect(Peer peer) {
    heartbeat.stop(peer);

    String key = peer.address();
    Channel ch = channels.remove(key);
    if (ch != null) ch.close();

    NettyClient client = clients.remove(key);
    if (client != null) client.close();
  }

  @Override
  public void close() {
    heartbeat.close();

    for (NettyClient c : clients.values()) {
      try { c.close(); } catch (Throwable ignore) {}
    }
    clients.clear();

    for (Channel ch : channels.values()) {
      try { ch.close(); } catch (Throwable ignore) {}
    }
    channels.clear();

    sharedGroup.shutdownGracefully().syncUninterruptibly();
  }
}
