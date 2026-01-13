// com/ht/p2p/core/router/RpcKernel.java
package com.ht.p2p.core.router;

import com.ht.p2p.core.transport.Session;
import com.ht.p2p.core.scheduler.Scheduler;
import com.ht.p2p.proto.Envelope;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RpcKernel implements AutoCloseable {

  private record Pending(CompletableFuture<Envelope> fut, Session session, ScheduledFuture<?> timeoutTask) {}

  private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
  private final Scheduler scheduler;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public RpcKernel(Scheduler scheduler) {
    this.scheduler = Objects.requireNonNull(scheduler);
  }

  public CompletableFuture<Envelope> register(String requestId, Session session, Duration timeout) {
    ensureOpen();
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(session);
    Objects.requireNonNull(timeout);

    CompletableFuture<Envelope> fut = new CompletableFuture<>();

    ScheduledFuture<?> task = scheduler.schedule(timeout, () -> {
      Pending removed = pending.remove(requestId);
      if (removed != null && !removed.fut().isDone()) {
        removed.fut().completeExceptionally(new TimeoutException("RPC timeout requestId=" + requestId));
      }
    });

    Pending p = new Pending(fut, session, task);
    Pending prev = pending.putIfAbsent(requestId, p);
    if (prev != null) {
      task.cancel(false);
      throw new IllegalStateException("Duplicate requestId: " + requestId);
    }

    fut.whenComplete((ok, err) -> {
      Pending cur = pending.remove(requestId);
      if (cur != null) cur.timeoutTask().cancel(false);
    });

    return fut;
  }

  public boolean complete(String requestId, Envelope env) {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(env);
    Pending p = pending.remove(requestId);
    if (p == null) return false;
    p.timeoutTask().cancel(false);
    return p.fut().complete(env);
  }

  /** Fail all inflight requests of a disconnected session (prevents hanging futures). */
  public void failAllForSession(Session session, Throwable cause) {
    Objects.requireNonNull(session);
    for (var e : pending.entrySet()) {
      Pending p = e.getValue();
      if (p.session() == session) {
        if (pending.remove(e.getKey(), p)) {
          p.timeoutTask().cancel(false);
          p.fut().completeExceptionally(cause != null ? cause : new CancellationException("session down"));
        }
      }
    }
  }

  public int size() { return pending.size(); }

  private void ensureOpen() {
    if (closed.get()) throw new IllegalStateException("RpcKernel closed");
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;
    for (var e : pending.entrySet()) {
      e.getValue().timeoutTask().cancel(false);
      e.getValue().fut().completeExceptionally(new CancellationException("RpcKernel closed"));
    }
    pending.clear();
  }
}
