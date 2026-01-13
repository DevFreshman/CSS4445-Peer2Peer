// com/ht/p2p/core/handshake/HandshakeService.java
package com.ht.p2p.core.handshake;

import com.google.protobuf.ByteString;
import com.ht.p2p.core.api.NodeConfig;
import com.ht.p2p.core.identity.NodeId;
import com.ht.p2p.core.router.RpcKernel;
import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.Transport;
import com.ht.p2p.proto.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class HandshakeService {

  private final NodeConfig cfg;
  private final Transport transport;
  private final RpcKernel rpc;

  public HandshakeService(NodeConfig cfg, Transport transport, RpcKernel rpc) {
    this.cfg = Objects.requireNonNull(cfg);
    this.transport = Objects.requireNonNull(transport);
    this.rpc = Objects.requireNonNull(rpc);
  }

  public CompletableFuture<Void> clientHandshake(Session session) {
    String requestId = UUID.randomUUID().toString();

    var fut = rpc.register(requestId, session, cfg.handshakeTimeout());

    HelloReq req = HelloReq.newBuilder()
        .setNodeId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setListenPort(cfg.listenPort())
        .setVersion(cfg.version())
        .build();

    Envelope out = Envelope.newBuilder()
        .setSenderId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setRequestId(requestId)
        .setTimestampMs(Instant.now().toEpochMilli())
        .setHelloReq(req)
        .build();

    transport.send(session, out);

    return fut.thenApply(env -> {
      if (!env.hasHelloRes()) throw new IllegalStateException("Expected HelloRes");
      HelloRes res = env.getHelloRes();
      if (!res.getOk()) throw new IllegalStateException("HelloRes not ok");
      NodeId remote = new NodeId(res.getNodeId().toByteArray());
      if (remote.equals(cfg.selfId())) throw new IllegalStateException("Remote nodeId == self");

      session.remoteId(remote);
      session.handshakeOk(true);
      return null;
    });
  }

  public void onHelloReq(Session session, Envelope env) {
    HelloReq req = env.getHelloReq();
    NodeId remote = new NodeId(req.getNodeId().toByteArray());
    session.remoteId(remote);

    HelloRes res = HelloRes.newBuilder()
        .setOk(true)
        .setNodeId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setVersion(cfg.version())
        .build();

    Envelope out = Envelope.newBuilder()
        .setSenderId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setRequestId(env.getRequestId()) // echo
        .setTimestampMs(System.currentTimeMillis())
        .setHelloRes(res)
        .build();

    transport.send(session, out);

    session.handshakeOk(true);
  }

  public void onHelloRes(Session session, Envelope env) {
    // Usually completed by rpc.complete() already.
    // If arrives without pending, you can still accept it:
    HelloRes res = env.getHelloRes();
    if (res.getOk()) {
      session.remoteId(new NodeId(res.getNodeId().toByteArray()));
      session.handshakeOk(true);
    }
  }

  public void requireHandshakeOk(Session session) {
    if (!session.handshakeOk()) throw new IllegalStateException("Handshake not completed for " + session.address());
  }

  public void onPingReq(Session session, Envelope env) {
    // For demo: respond pong
    PingRes res = PingRes.newBuilder().setOk(true).setMessage("pong").build();
    Envelope out = Envelope.newBuilder()
        .setSenderId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setRequestId(env.getRequestId())
        .setTimestampMs(System.currentTimeMillis())
        .setPingRes(res)
        .build();
    transport.send(session, out);
  }
}
