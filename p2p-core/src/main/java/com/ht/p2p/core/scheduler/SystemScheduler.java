// com/ht/p2p/core/scheduler/SystemScheduler.java
package com.ht.p2p.core.scheduler;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;

public final class SystemScheduler implements Scheduler {
  private final ScheduledExecutorService ses;

  public SystemScheduler(String threadName) {
    Objects.requireNonNull(threadName);
    this.ses = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, threadName);
      t.setDaemon(true);
      return t;
    });
  }

  @Override
  public ScheduledFuture<?> schedule(Duration delay, Runnable task) {
    return ses.schedule(task, Math.max(1, delay.toMillis()), TimeUnit.MILLISECONDS);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Duration initialDelay, Duration period, Runnable task) {
    return ses.scheduleAtFixedRate(
        task,
        Math.max(1, initialDelay.toMillis()),
        Math.max(1, period.toMillis()),
        TimeUnit.MILLISECONDS
    );
  }

  @Override
  public void close() {
    ses.shutdownNow();
  }
}
