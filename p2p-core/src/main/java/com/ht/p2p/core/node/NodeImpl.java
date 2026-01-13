// com/ht/p2p/core/node/NodeImpl.java
package com.ht.p2p.core.node;

import com.ht.p2p.core.api.Node;
import com.ht.p2p.core.api.NodeConfig;
import com.ht.p2p.core.dht.DhtService;
import com.ht.p2p.core.gossip.GossipService;
import com.ht.p2p.core.handshake.HandshakeService;
import com.ht.p2p.core.heartbeat.HeartbeatService;
import com.ht.p2p.core.router.MessageRouter;
import com.ht.p2p.core.router.RpcKernel;
import com.ht.p2p.core.scheduler.Scheduler;
import com.ht.p2p.core.scheduler.SystemScheduler;
import com.ht.p2p.core.security.MessageValidator;
import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.Transport;
import com.ht.p2p.core.transport.TransportListener;
import com.ht.p2p.core.rpc.RpcClient;
import com.ht.p2p.proto.PingRes;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class NodeImpl implements Node, TransportListener {

  private final NodeConfig cfg;
  private final Transport transport;
  private final RpcClient rpcClient;

  private final Scheduler scheduler = new SystemScheduler("p2p-core-scheduler");
  private final RpcKernel rpc = new RpcKernel(scheduler);

  private final MessageValidator validator = new MessageValidator();

  private final HandshakeService handshake;
  private final GossipService gossip;
  private final DhtService dht;
  private final HeartbeatService heartbeat;

  private final MessageRouter router;

  private volatile boolean started = false;

  public NodeImpl(NodeConfig cfg, Transport transport) {
    this.cfg = Objects.requireNonNull(cfg);
    this.transport = Objects.requireNonNull(transport);

    this.handshake = new HandshakeService(cfg, transport, rpc);
    this.gossip = new GossipService(cfg, transport);
    this.dht = new DhtService(cfg);
    this.heartbeat = new HeartbeatService(cfg, transport, rpc, scheduler);
    this.rpcClient = new RpcClient(cfg, transport, rpc);

    this.router = new MessageRouter(rpc, handshake, gossip, validator);
  }

  @Override
  public CompletableFuture<Void> start() {
    if (started) return CompletableFuture.completedFuture(null);
    started = true;
    return transport.startListener(cfg.listenPort(), this);
  }

  @Override
  public CompletableFuture<Void> stop() {
    close();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Session> connect(String host, int port) {
    return transport.connect(host, port)
        .thenCompose(sess -> handshake.clientHandshake(sess).thenApply(ignored -> {
          heartbeat.start(sess); // start AFTER handshake ok
          return sess;
        }));
  }

  @Override
public CompletableFuture<Boolean> ping(String host, int port, String message) {
  return connect(host, port)
      .thenCompose(sess -> rpcClient.ping(sess, message, cfg.rpcDefaultTimeout()))
      .thenApply(PingRes::getOk);
}

  @Override
  public CompletableFuture<Void> bootstrap(List<HostPort> seeds) {
    if (seeds == null || seeds.isEmpty()) return CompletableFuture.completedFuture(null);
    // naive: connect all
    var all = seeds.stream()
        .map(s -> connect(s.host(), s.port()).thenApply(x -> null))
        .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(all);
  }

  @Override
  public CompletableFuture<Void> gossipPublish(String topic, byte[] payload) {
    return gossip.publish(topic, payload);
  }

  @Override
  public CompletableFuture<Void> dhtPut(byte[] key, byte[] value) {
    return dht.put(key, value);
  }

  @Override
  public CompletableFuture<byte[]> dhtGet(byte[] key) {
    return dht.get(key);
  }

  // ===== TransportListener =====

  @Override
  public void onConnected(Session session) {
    // server-side accepted connection: wait for HelloReq, router will handle
  }

  @Override
  public void onDisconnected(Session session, Throwable cause) {
    heartbeat.stop(session);
    rpc.failAllForSession(session, cause);
  }

  @Override
  public void onMessage(Session session, com.ht.p2p.proto.Envelope envelope) {
    router.onInbound(session, envelope);
  }

  @Override
  public void close() {
    try { heartbeat.close(); } catch (Throwable ignore) {}
    try { rpc.close(); } catch (Throwable ignore) {}
    try { scheduler.close(); } catch (Throwable ignore) {}
    try { transport.close(); } catch (Throwable ignore) {}
    started = false;
  }
}
