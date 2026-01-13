package com.ht.p2p.net.rpc;

import com.ht.p2p.proto.Envelope;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PendingRequests:
 * - Map requestId -> CompletableFuture<Envelope>
 * - register() schedules a timeout that removes the entry & completes exceptionally
 * - complete() removes & completes normally
 *
 * This is the "kernel" of RPC correlation.
 */
public final class PendingRequests implements AutoCloseable {

  private final ConcurrentHashMap<String, CompletableFuture<Envelope>> pending = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public PendingRequests() {
    // 1 thread is enough: only schedules timeouts
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "p2p-pending-timeouts");
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * Register a requestId with timeout.
   * Returns the future that will be completed by complete().
   */
  public CompletableFuture<Envelope> register(String requestId, Duration timeout) {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(timeout, "timeout");
    ensureOpen();

    CompletableFuture<Envelope> fut = new CompletableFuture<>();

    CompletableFuture<Envelope> prev = pending.putIfAbsent(requestId, fut);
    if (prev != null) {
      throw new IllegalStateException("Duplicate requestId registered: " + requestId);
    }

    long timeoutMs = Math.max(1L, timeout.toMillis());

    scheduler.schedule(() -> {
      // If still pending, remove it and fail
      CompletableFuture<Envelope> removed = pending.remove(requestId);
      if (removed != null && !removed.isDone()) {
        removed.completeExceptionally(new TimeoutException("RPC timeout requestId=" + requestId + " after " + timeoutMs + "ms"));
      }
    }, timeoutMs, TimeUnit.MILLISECONDS);

    // If caller cancels manually, also cleanup map
    fut.whenComplete((ok, err) -> pending.remove(requestId, fut));

    return fut;
  }

  /**
   * Complete by requestId. Returns true if a pending future existed and is completed.
   */
  public boolean complete(String requestId, Envelope envelope) {
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(envelope, "envelope");

    CompletableFuture<Envelope> fut = pending.remove(requestId);
    if (fut == null) return false;
    return fut.complete(envelope);
  }

  /**
   * For observability / debug.
   */
  public int size() {
    return pending.size();
  }

  private void ensureOpen() {
    if (closed.get()) throw new IllegalStateException("PendingRequests is closed");
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;

    // fail all in-flight
    for (var e : pending.entrySet()) {
      e.getValue().completeExceptionally(new CancellationException("PendingRequests closed"));
    }
    pending.clear();

    scheduler.shutdownNow();
  }
}
