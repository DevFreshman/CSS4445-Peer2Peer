// com/ht/p2p/core/router/MessageRouter.java
package com.ht.p2p.core.router;

import com.ht.p2p.core.gossip.GossipService;
import com.ht.p2p.core.handshake.HandshakeService;
import com.ht.p2p.core.security.MessageValidator;
import com.ht.p2p.core.transport.Session;
import com.ht.p2p.proto.Envelope;

import java.util.Objects;

/**
 * Core inbound router:
 * 1) validate
 * 2) if RPC response -> rpc.complete
 * 3) else dispatch by payload type
 */
public final class MessageRouter {

  private final RpcKernel rpc;
  private final HandshakeService handshake;
  private final GossipService gossip;
  private final MessageValidator validator;

  public MessageRouter(RpcKernel rpc,
                       HandshakeService handshake,
                       GossipService gossip,
                       MessageValidator validator) {
    this.rpc = Objects.requireNonNull(rpc);
    this.handshake = Objects.requireNonNull(handshake);
    this.gossip = Objects.requireNonNull(gossip);
    this.validator = Objects.requireNonNull(validator);
  }

  public void onInbound(Session session, Envelope env) {
    validator.validate(session, env);

    // 1) RPC response fast-path
    if (!env.getRequestId().isEmpty()) {
      boolean completed = rpc.complete(env.getRequestId(), env);
      if (completed) return;
    }

    // 2) Dispatch
    if (env.hasHelloReq()) {
      handshake.onHelloReq(session, env);
      return;
    }
    if (env.hasHelloRes()) {
      handshake.onHelloRes(session, env);
      return;
    }
    if (env.hasPingReq()) {
      handshake.requireHandshakeOk(session); // policy: only handle after handshake
      handshake.onPingReq(session, env);
      return;
    }
    if (env.hasPingRes()) {
      // pingres without pending: ignore or log
      return;
    }

    // gossip placeholder (you'll add proto later)
    // if (env.hasGossipMsg()) { gossip.onGossip(session, env.getGossipMsg()); return; }

    // unknown: ignore/log
  }
}
