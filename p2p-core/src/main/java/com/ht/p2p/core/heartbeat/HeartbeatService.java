// com/ht/p2p/core/heartbeat/HeartbeatService.java
package com.ht.p2p.core.heartbeat;

import com.google.protobuf.ByteString;
import com.ht.p2p.core.api.NodeConfig;
import com.ht.p2p.core.router.RpcKernel;
import com.ht.p2p.core.scheduler.Scheduler;
import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.transport.Transport;
import com.ht.p2p.proto.Envelope;
import com.ht.p2p.proto.PingReq;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class HeartbeatService implements AutoCloseable {
  public static final String HB_MSG = "__hb__";

  private final NodeConfig cfg;
  private final Transport transport;
  private final RpcKernel rpc;
  private final Scheduler scheduler;

  private final Map<Session, State> states = new ConcurrentHashMap<>();

  public HeartbeatService(NodeConfig cfg, Transport transport, RpcKernel rpc, Scheduler scheduler) {
    this.cfg = Objects.requireNonNull(cfg);
    this.transport = Objects.requireNonNull(transport);
    this.rpc = Objects.requireNonNull(rpc);
    this.scheduler = Objects.requireNonNull(scheduler);
  }

  public void start(Session session) {
    stop(session);
    State st = new State(session);
    states.put(session, st);

    st.task = scheduler.scheduleAtFixedRate(cfg.heartbeatInterval(), cfg.heartbeatInterval(), () -> tick(st));
  }

  public void stop(Session session) {
    State st = states.remove(session);
    if (st != null) st.cancel();
  }

  private void tick(State st) {
    if (!st.session.handshakeOk()) return;

    String requestId = UUID.randomUUID().toString();
    var fut = rpc.register(requestId, st.session, cfg.heartbeatTimeout());

    PingReq req = PingReq.newBuilder().setMessage(HB_MSG).build();
    Envelope env = Envelope.newBuilder()
        .setSenderId(ByteString.copyFrom(cfg.selfId().bytes()))
        .setRequestId(requestId)
        .setTimestampMs(Instant.now().toEpochMilli())
        .setPingReq(req)
        .build();

    transport.send(st.session, env);

    fut.thenAccept(ok -> st.fail.set(0))
       .exceptionally(err -> {
         int fc = st.fail.incrementAndGet();
         if (fc >= cfg.heartbeatFailThreshold()) {
           transport.close(st.session);
           stop(st.session);
         }
         return null;
       });
  }

  @Override
  public void close() {
    for (var e : states.entrySet()) e.getValue().cancel();
    states.clear();
  }

  private static final class State {
    final Session session;
    final AtomicInteger fail = new AtomicInteger(0);
    volatile ScheduledFuture<?> task;

    State(Session session) { this.session = session; }

    void cancel() {
      if (task != null) {
        task.cancel(false);
        task = null;
      }
    }
  }
}
