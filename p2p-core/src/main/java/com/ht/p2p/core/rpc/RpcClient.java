package com.ht.p2p.core.rpc;

import com.google.protobuf.ByteString;
import com.ht.p2p.core.api.NodeConfig;
import com.ht.p2p.core.router.RpcKernel;
import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.Transport;
import com.ht.p2p.proto.Envelope;
import com.ht.p2p.proto.PingReq;
import com.ht.p2p.proto.PingRes;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RpcClient {
  private final NodeConfig cfg;
  private final Transport transport;
  private final RpcKernel rpc;

  public RpcClient(NodeConfig cfg, Transport transport, RpcKernel rpc) {
    this.cfg = Objects.requireNonNull(cfg);
    this.transport = Objects.requireNonNull(transport);
    this.rpc = Objects.requireNonNull(rpc);
  }

  public CompletableFuture<PingRes> ping(Session session, String message, Duration timeout) {
    Objects.requireNonNull(session);
    Objects.requireNonNull(message);
    Objects.requireNonNull(timeout);

    String requestId = UUID.randomUUID().toString();
    CompletableFuture<Envelope> fut = rpc.register(requestId, session, timeout);

    PingReq req = PingReq.newBuilder().setMessage(message).build();

    Envelope env = Envelope.newBuilder()
        .setSenderId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setRequestId(requestId)
        .setTimestampMs(Instant.now().toEpochMilli())
        .setPingReq(req)
        .build();

    transport.send(session, env);

    return fut.thenApply(e -> {
      if (!e.hasPingRes()) {
        throw new IllegalStateException("Expected PingRes for requestId=" + requestId);
      }
      return e.getPingRes();
    });
  }
}
