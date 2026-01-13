package com.ht.p2p.net.conn;

import com.google.protobuf.ByteString;
import com.ht.p2p.net.peer.Peer;
import com.ht.p2p.net.rpc.PendingRequests;
import com.ht.p2p.proto.Envelope;
import com.ht.p2p.proto.PingReq;
import io.netty.channel.Channel;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Heartbeat / liveness:
 * - every interval send PingReq("__hb__") over existing channel
 * - wait PingRes via PendingRequests (timeout)
 * - failThreshold times => close channel
 *
 * Không phụ thuộc RpcClient để tránh vòng lặp dependency.
 */
public final class HeartbeatService implements AutoCloseable {

  public static final String HB_MSG = "__hb__";

  private final PendingRequests pending;
  private final ByteString selfId;

  private final Duration interval;
  private final Duration timeout;
  private final int failThreshold;

  private final Map<String, State> states = new ConcurrentHashMap<>();

  public HeartbeatService(PendingRequests pending, ByteString selfId) {
    this(pending, selfId, Duration.ofSeconds(5), Duration.ofSeconds(2), 3);
  }

  public HeartbeatService(PendingRequests pending, ByteString selfId,
                          Duration interval, Duration timeout, int failThreshold) {
    this.pending = Objects.requireNonNull(pending);
    this.selfId = Objects.requireNonNull(selfId);
    this.interval = Objects.requireNonNull(interval);
    this.timeout = Objects.requireNonNull(timeout);
    this.failThreshold = failThreshold;
  }

  public void start(Peer peer, Channel ch) {
    Objects.requireNonNull(peer);
    Objects.requireNonNull(ch);

    String key = peer.address();
    stop(peer); // clear old if exists

    State st = new State(peer, ch);
    states.put(key, st);

    st.future = ch.eventLoop().scheduleAtFixedRate(
        () -> tick(st),
        interval.toMillis(),
        interval.toMillis(),
        TimeUnit.MILLISECONDS
    );

    // channel close => stop heartbeat state
    ch.closeFuture().addListener(ignored -> stop(peer));
  }

  public void stop(Peer peer) {
    if (peer == null) return;
    State st = states.remove(peer.address());
    if (st != null) st.cancel();
  }

  private void tick(State st) {
    if (!st.ch.isActive()) {
      st.cancel();
      return;
    }

    String requestId = UUID.randomUUID().toString();
    var raw = pending.register(requestId, timeout);

    PingReq req = PingReq.newBuilder().setMessage(HB_MSG).build();
    Envelope env = Envelope.newBuilder()
        .setSenderId(selfId)
        .setRequestId(requestId)
        .setTimestampMs(Instant.now().toEpochMilli())
        .setPingReq(req)
        .build();

    st.ch.writeAndFlush(env).addListener(f -> {
      if (!f.isSuccess()) raw.completeExceptionally(f.cause());
    });

    raw.thenAccept(ignored -> st.failCount.set(0))
       .exceptionally(err -> {
         int fc = st.failCount.incrementAndGet();
         if (fc >= failThreshold) {
           st.ch.close();   // close channel triggers cleanup ở ConnectionManager
           st.cancel();
         }
         return null;
       });
  }

  @Override
  public void close() {
    for (State st : states.values()) st.cancel();
    states.clear();
  }

  private static final class State {
    final Peer peer;
    final Channel ch;
    final AtomicInteger failCount = new AtomicInteger(0);
    volatile io.netty.util.concurrent.ScheduledFuture<?> future;

    State(Peer peer, Channel ch) {
      this.peer = peer;
      this.ch = ch;
    }

    void cancel() {
      if (future != null) {
        future.cancel(false);
        future = null;
      }
    }
  }
}
