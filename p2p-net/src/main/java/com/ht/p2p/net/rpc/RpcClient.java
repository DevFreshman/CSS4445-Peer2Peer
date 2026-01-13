package com.ht.p2p.net.rpc;

import com.google.protobuf.ByteString;
import com.ht.p2p.net.conn.ConnectionManager;
import com.ht.p2p.net.peer.Peer;
import com.ht.p2p.proto.Envelope;
import com.ht.p2p.proto.HelloReq;
import com.ht.p2p.proto.HelloRes;
import com.ht.p2p.proto.PingReq;
import com.ht.p2p.proto.PingRes;
import io.netty.channel.Channel;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RpcClient {

  private final PendingRequests pending;
  private final ConnectionManager connections;
  private final ByteString selfId;

  public RpcClient(PendingRequests pending, ConnectionManager connections, ByteString selfId) {
    this.pending = Objects.requireNonNull(pending);
    this.connections = Objects.requireNonNull(connections);
    this.selfId = Objects.requireNonNull(selfId);
  }

  // ===== public API =====

  public CompletableFuture<PingRes> ping(Peer peer, String message, Duration timeout) {
    return connections.getOrConnect(peer).thenCompose(ch -> pingOnChannel(ch, message, timeout));
  }

  public CompletableFuture<HelloRes> hello(Peer peer, int listenPort, Duration timeout) {
    return connections.getOrConnect(peer).thenCompose(ch -> helloOnChannel(ch, listenPort, timeout));
  }

  // ===== channel-level API (NO getOrConnect inside) =====

  public CompletableFuture<PingRes> pingOnChannel(Channel ch, String message, Duration timeout) {
    String requestId = UUID.randomUUID().toString();
    CompletableFuture<Envelope> rawFuture = pending.register(requestId, timeout);

    PingReq req = PingReq.newBuilder().setMessage(message).build();

    Envelope env = Envelope.newBuilder()
        .setSenderId(selfId)
        .setRequestId(requestId)
        .setTimestampMs(Instant.now().toEpochMilli())
        .setPingReq(req)
        .build();

    ch.writeAndFlush(env).addListener(f -> {
      if (!f.isSuccess()) rawFuture.completeExceptionally(f.cause());
    });

    return rawFuture.thenApply(Envelope::getPingRes);
  }

  public CompletableFuture<HelloRes> helloOnChannel(Channel ch, int listenPort, Duration timeout) {
    String requestId = UUID.randomUUID().toString();
    CompletableFuture<Envelope> rawFuture = pending.register(requestId, timeout);

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
      if (!f.isSuccess()) rawFuture.completeExceptionally(f.cause());
    });

    return rawFuture.thenApply(Envelope::getHelloRes);
  }
}
